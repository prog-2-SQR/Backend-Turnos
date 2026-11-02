package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.messaging.publisher;

import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.NOW;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.phoneRequested;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import ar.edu.um.prog2.quiroga.turnos.config.PhoneOutboxProperties;
import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.PhoneSubmission;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.messaging.mapper.PhoneSubmittedMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class KafkaPhoneSubmissionPublisherTest {

    private static final String TOPIC = "catedra.turnos.telefono.test";

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private KafkaPhoneSubmissionPublisher publisher;

    private final PhoneSubmission submission = PhoneSubmission.of(phoneRequested(), "+5492615551234", NOW);

    @BeforeEach
    void setUp() {
        PhoneOutboxProperties properties = new PhoneOutboxProperties();
        properties.setGroupId("proyecto-juan-perez");
        properties.setTopic(TOPIC);
        properties.setSendTimeout(Duration.ofMillis(200));
        publisher = new KafkaPhoneSubmissionPublisher(kafkaTemplate, jsonMapper, new PhoneSubmittedMapper(), properties);
    }

    @Test
    void publishesTheEventOfTheAnnexWithTheCatedraProcessAsKey() {
        when(kafkaTemplate.send(eq(TOPIC), eq("process-a92b47d0"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(new SendResult<>(null, null)));

        publisher.publish(submission);

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(TOPIC), eq("process-a92b47d0"), payload.capture());
        JsonNode event = jsonMapper.readTree(payload.getValue());
        assertThat(event.get("eventId").asString()).isEqualTo(submission.getEventId());
        assertThat(event.get("eventType").asString()).isEqualTo("AdditionalInformationSubmitted");
        assertThat(event.get("groupId").asString()).isEqualTo("proyecto-juan-perez");
        assertThat(event.get("reservationProcessId").asString()).isEqualTo("process-a92b47d0");
        assertThat(event.get("holdId").asString()).isEqualTo("hold-6c0c2f41");
        assertThat(event.get("requestEventId").asString()).isEqualTo("request-event-8a31");
        assertThat(event.get("information").get("phoneNumber").asString()).isEqualTo("+5492615551234");
        assertThat(event.get("occurredAt").asString()).isEqualTo(NOW.toString());
        assertThat(event.get("schemaVersion").asInt()).isEqualTo(1);
    }

    @Test
    void anUnconfirmedSendIsAnError() {
        when(kafkaTemplate.send(eq(TOPIC), eq("process-a92b47d0"), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker caído")));

        assertThatThrownBy(() -> publisher.publish(submission))
                .isInstanceOf(ReservationException.class)
                .extracting("code").isEqualTo(ReservationException.CATEDRA_UNAVAILABLE);
    }

    @Test
    void aSendWithoutAnswerInTimeIsAnError() {
        when(kafkaTemplate.send(eq(TOPIC), eq("process-a92b47d0"), anyString())).thenReturn(new CompletableFuture<>());

        assertThatThrownBy(() -> publisher.publish(submission))
                .isInstanceOf(ReservationException.class)
                .extracting("code").isEqualTo(ReservationException.CATEDRA_UNAVAILABLE);
    }
}
