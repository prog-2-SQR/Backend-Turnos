package ar.edu.um.prog2.quiroga.turnos.reservation;

import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.NOW;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.held;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.waitingForPhone;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;

import ar.edu.um.prog2.quiroga.turnos.TestSecrets;
import ar.edu.um.prog2.quiroga.turnos.TestcontainersConfiguration;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/** Del topic de acciones a la base: Kafka y PostgreSQL reales, listener prendido. */
@SpringBootTest(properties = {"security.jwt.base64-secret=" + TestSecrets.JWT_SECRET,
        "spring.kafka.listener.auto-startup=true", "catedra.retry-delay=500ms", "catedra.max-retries=6"})
@ExtendWith(OutputCaptureExtension.class)
@Import({TestcontainersConfiguration.class, ReservationActionsKafkaTest.KafkaConfiguration.class})
class ReservationActionsKafkaTest {

    private static final String TOPIC = "alumnos.turnos.acciones.test";
    private static final Duration WAIT = Duration.ofSeconds(30);

    @TestConfiguration(proxyBeanMethods = false)
    static class KafkaConfiguration {

        // Imagen JVM: la nativa (apache/kafka-native) se cayó con un segfault en el runner del CI.
        @Bean
        @ServiceConnection
        KafkaContainer kafkaContainer() {
            return new KafkaContainer(DockerImageName.parse("apache/kafka:4.1.1"));
        }

        // KafkaAdmin crea el topic al arrancar, antes de que el listener se suscriba.
        @Bean
        NewTopic actionsTopic() {
            return new NewTopic(TOPIC, 1, (short) 1);
        }
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ReservationProcessRepository reservationProcessRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void clean() {
        jdbcTemplate.execute("TRUNCATE reservation_process, reservation_processed_events");
    }

    @Test
    void aRepeatedPhoneRequestIsSavedOnlyOnce() {
        ReservationProcess process = reservationProcessRepository.saveReservationProcess(waitingForPhone());

        publish(phoneRequested("request-event-8a31", "process-a92b47d0"));
        publish(phoneRequested("request-event-8a31", "process-a92b47d0"));
        // Una partición: este llega después del repetido, así que cuando se registra, el repetido ya pasó.
        publish(phoneRequested("request-event-otro", "process-desconocido"));

        await().atMost(WAIT).untilAsserted(() -> assertThat(processedEventIds())
                .containsExactlyInAnyOrder("request-event-8a31", "request-event-otro"));
        ReservationProcess updated = reservationProcessRepository.findReservationProcessById(process.getId()).orElseThrow();
        assertThat(updated.isPhoneRequested()).isTrue();
        assertThat(updated.getPhoneRequestEventId()).isEqualTo("request-event-8a31");
        // Guardado una sola vez: la versión subió de 0 (creación) a 1 (pedido de teléfono).
        assertThat(updated.getVersion()).isEqualTo(1L);
    }

    @Test
    void aRequestArrivingBeforeTheConfirmationIsSavedIsRetried(CapturedOutput output) throws Exception {
        ReservationProcess process = reservationProcessRepository.saveReservationProcess(held());

        publish(phoneRequested("request-event-8a31", "process-a92b47d0"));
        // El listener lo intenta en seguida y cada 500 ms: los primeros intentos encuentran el proceso todavía en HELD.
        await().atMost(WAIT).until(() -> output.getOut().contains("llegó antes de que se guardara la confirmación"));
        // La respuesta REST de la confirmación se guarda un momento después que el evento.
        process.waitForPhone(null, NOW);
        reservationProcessRepository.saveReservationProcess(process);

        await().atMost(WAIT).untilAsserted(() -> assertThat(
                reservationProcessRepository.findReservationProcessById(process.getId()).orElseThrow().isPhoneRequested()).isTrue());
    }

    // Ejemplo del anexo §15.4. Message key: reservationProcessId (anexo §15.1).
    private void publish(String[] keyAndPayload) {
        kafkaTemplate.send(TOPIC, keyAndPayload[0], keyAndPayload[1]).join();
    }

    private static String[] phoneRequested(String eventId, String catedraProcessId) {
        return new String[] {catedraProcessId, """
                {"eventId": "%s", "eventType": "AdditionalInformationRequested", "reservationProcessId": "%s",
                 "holdId": "hold-6c0c2f41", "requiredField": "PHONE_NUMBER",
                 "message": "Ingrese un numero de telefono para continuar.", "schemaVersion": 1}""".formatted(eventId, catedraProcessId)};
    }

    private List<String> processedEventIds() {
        return jdbcTemplate.queryForList("SELECT event_id FROM reservation_processed_events", String.class);
    }
}
