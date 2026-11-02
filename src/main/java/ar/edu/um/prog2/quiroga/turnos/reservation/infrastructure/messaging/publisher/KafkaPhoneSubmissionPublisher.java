package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.messaging.publisher;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import ar.edu.um.prog2.quiroga.turnos.config.PhoneOutboxProperties;
import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.PhoneSubmission;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.PhoneSubmissionPublisher;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.messaging.mapper.PhoneSubmittedMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.KafkaException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Publica AdditionalInformationSubmitted en el topic del teléfono (anexo §15.2 y §15.5). La message key es el
 * reservationProcessId de la cátedra, para que los eventos de un mismo proceso vayan a la misma partición (anexo §15.1).
 * Espera a que Kafka confirme la recepción: si no llega a tiempo, el envío cuenta como fallido y se reintenta.
 */
@Component
@RequiredArgsConstructor
public class KafkaPhoneSubmissionPublisher implements PhoneSubmissionPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;
    private final PhoneSubmittedMapper phoneSubmittedMapper;
    private final PhoneOutboxProperties phoneOutboxProperties;

    @Override
    public void publish(PhoneSubmission submission) {
        String payload = jsonMapper.writeValueAsString(phoneSubmittedMapper.toMessage(submission, phoneOutboxProperties.getGroupId()));
        try {
            kafkaTemplate.send(phoneOutboxProperties.getTopic(), submission.getCatedraProcessId(), payload)
                    .get(phoneOutboxProperties.getSendTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable(e);
        } catch (ExecutionException | TimeoutException | KafkaException e) {
            throw unavailable(e);
        }
    }

    private static ReservationException unavailable(Exception cause) {
        return new ReservationException(ReservationException.CATEDRA_UNAVAILABLE, "Kafka no confirmó el envío del teléfono", cause);
    }
}
