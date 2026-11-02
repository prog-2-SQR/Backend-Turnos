package ar.edu.um.prog2.quiroga.turnos.reservation;

import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.NOW;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.phoneRequested;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.phoneSubmitted;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ar.edu.um.prog2.quiroga.turnos.TestSecrets;
import ar.edu.um.prog2.quiroga.turnos.TestcontainersConfiguration;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.PhoneSubmission;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationStatus;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.PhoneSubmissionOutbox;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Pasos 5 a 7 del enunciado §7 con Kafka y PostgreSQL reales: el teléfono sale del outbox al topic del teléfono, y los
 * resultados que llegan por el topic de acciones cambian el proceso. El outbox se revisa cada 300 ms.
 */
@SpringBootTest(properties = {"security.jwt.base64-secret=" + TestSecrets.JWT_SECRET,
        "spring.kafka.listener.auto-startup=true", "catedra.phone-outbox.interval=PT0.3S"})
@Import({TestcontainersConfiguration.class, ReservationPhoneKafkaTest.KafkaConfiguration.class})
class ReservationPhoneKafkaTest {

    private static final String ACTIONS_TOPIC = "alumnos.turnos.acciones.test";
    private static final String PHONE_TOPIC = "catedra.turnos.telefono.test";
    private static final Duration WAIT = Duration.ofSeconds(30);

    @TestConfiguration(proxyBeanMethods = false)
    static class KafkaConfiguration {

        // Imagen JVM: la nativa (apache/kafka-native) se cayó con un segfault en el runner del CI.
        @Bean
        @ServiceConnection
        KafkaContainer kafkaContainer() {
            return new KafkaContainer(DockerImageName.parse("apache/kafka:4.1.1"));
        }

        @Bean
        NewTopic actionsTopic() {
            return new NewTopic(ACTIONS_TOPIC, 1, (short) 1);
        }

        @Bean
        NewTopic phoneTopic() {
            return new NewTopic(PHONE_TOPIC, 1, (short) 1);
        }
    }

    @Autowired
    private KafkaContainer kafkaContainer;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ReservationProcessRepository reservationProcessRepository;

    @Autowired
    private PhoneSubmissionOutbox phoneSubmissionOutbox;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void clean() {
        jdbcTemplate.execute("TRUNCATE reservation_process, reservation_processed_events CASCADE");
    }

    @Test
    void thePhoneIsPublishedAsInTheAnnexAndTheConfirmationClosesTheProcess() {
        // Lo mismo que deja SubmitPhoneUseCase: el proceso en PHONE_SUBMITTED y el teléfono en el outbox.
        ReservationProcess process = phoneRequested();
        process.submitPhone(NOW);
        process = reservationProcessRepository.saveReservationProcess(process);
        PhoneSubmission submission = PhoneSubmission.of(process, "+5492615551234", NOW);
        phoneSubmissionOutbox.savePhoneSubmission(submission);

        ConsumerRecord<String, String> published = firstRecordOf(PHONE_TOPIC);

        assertThat(published.key()).isEqualTo("process-a92b47d0");
        JsonNode event = jsonMapper.readTree(published.value());
        assertThat(event.get("eventId").asString()).isEqualTo(submission.getEventId());
        assertThat(event.get("eventType").asString()).isEqualTo("AdditionalInformationSubmitted");
        assertThat(event.get("groupId").asString()).isEqualTo("grupo-de-prueba");
        assertThat(event.get("requestEventId").asString()).isEqualTo("request-event-8a31");
        assertThat(event.get("information").get("phoneNumber").asString()).isEqualTo("+5492615551234");
        await().atMost(WAIT).untilAsserted(() -> assertThat(sentAt(submission.getEventId())).isNotNull());

        publish("confirmation-event-7d10", "AppointmentConfirmed", ", \"reservationId\": 8251");

        UUID id = process.getId();
        await().atMost(WAIT).untilAsserted(() -> assertThat(statusOf(id)).isEqualTo(ReservationStatus.CONFIRMED));
        assertThat(reservationProcessRepository.findReservationProcessById(id).orElseThrow().getCatedraReservationId())
                .isEqualTo(8251L);
    }

    @Test
    void aRejectedPhoneCanBeSentAgainAndAFinalStateDoesNotGoBack() {
        UUID id = reservationProcessRepository.saveReservationProcess(phoneSubmitted()).getId();

        publish("rejection-event-7d11", "AdditionalInformationRejected", ", \"message\": \"El telefono ingresado no es valido.\"");
        await().atMost(WAIT).untilAsserted(() -> assertThat(statusOf(id)).isEqualTo(ReservationStatus.WAITING_FOR_PHONE));
        ReservationProcess rejected = reservationProcessRepository.findReservationProcessById(id).orElseThrow();
        assertThat(rejected.isPhoneRequested()).isTrue();
        assertThat(rejected.getPhoneRejectionMessage()).isEqualTo("El telefono ingresado no es valido.");

        publish("process-event-7d12", "AppointmentProcessExpired", ", \"reason\": \"HOLD_EXPIRED\"");
        publish("process-event-7d12", "AppointmentProcessExpired", ", \"reason\": \"HOLD_EXPIRED\"");
        // Una partición: la confirmación tardía llega después del vencimiento y no lo reabre.
        publish("confirmation-event-7d10", "AppointmentConfirmed", ", \"reservationId\": 8251");

        await().atMost(WAIT).untilAsserted(() -> assertThat(processedEventIds())
                .containsExactlyInAnyOrder("rejection-event-7d11", "process-event-7d12", "confirmation-event-7d10"));
        ReservationProcess expired = reservationProcessRepository.findReservationProcessById(id).orElseThrow();
        assertThat(expired.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(expired.getFailureCode()).isEqualTo("HOLD_EXPIRED");
        assertThat(expired.getCatedraReservationId()).isNull();
    }

    // Eventos del anexo §15.6 a §15.10 para el proceso del anexo. Message key: reservationProcessId (anexo §15.1).
    private void publish(String eventId, String eventType, String extraFields) {
        kafkaTemplate.send(ACTIONS_TOPIC, "process-a92b47d0", """
                {"eventId": "%s", "eventType": "%s", "groupId": "grupo-de-prueba", "reservationProcessId": "process-a92b47d0",
                 "holdId": "hold-6c0c2f41", "requestEventId": "request-event-8a31", "schemaVersion": 1%s}"""
                .formatted(eventId, eventType, extraFields)).join();
    }

    private ConsumerRecord<String, String> firstRecordOf(String topic) {
        Map<String, Object> config = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(config, new StringDeserializer(), new StringDeserializer())) {
            consumer.subscribe(List.of(topic));
            List<ConsumerRecord<String, String>> records = new ArrayList<>();
            await().atMost(WAIT).until(() -> {
                consumer.poll(Duration.ofMillis(200)).forEach(records::add);
                return !records.isEmpty();
            });
            return records.getFirst();
        }
    }

    private ReservationStatus statusOf(UUID id) {
        return reservationProcessRepository.findReservationProcessById(id).orElseThrow().getStatus();
    }

    private Object sentAt(String eventId) {
        return jdbcTemplate.queryForObject("SELECT sent_at FROM reservation_phone_outbox WHERE event_id = ?", Object.class, eventId);
    }

    private List<String> processedEventIds() {
        return jdbcTemplate.queryForList("SELECT event_id FROM reservation_processed_events", String.class);
    }
}
