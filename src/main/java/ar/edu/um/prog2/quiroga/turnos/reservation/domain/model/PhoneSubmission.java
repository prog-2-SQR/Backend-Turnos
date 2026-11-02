package ar.edu.um.prog2.quiroga.turnos.reservation.domain.model;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un teléfono para enviarle a la cátedra (AdditionalInformationSubmitted, anexo §15.5), guardado en el outbox en la
 * misma transacción que el cambio del proceso. Se publica después; si falla, se reintenta con el MISMO eventId y la
 * cátedra ignora los duplicados (respuesta del profe en Slack).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhoneSubmission {

    static final String EVENT_ID_PREFIX = "phone-submitted-";

    private String eventId;
    private UUID reservationProcessId;
    private String catedraProcessId;
    private String holdId;
    /** El eventId del AdditionalInformationRequested que se responde. */
    private String requestEventId;
    private String phoneNumber;
    private Instant createdAt;
    /** null hasta que Kafka confirma la recepción. */
    private Instant sentAt;
    private int attempts;

    /** El teléfono que respondió el usuario para este proceso, con un eventId nuevo. */
    public static PhoneSubmission of(ReservationProcess process, String phoneNumber, Instant now) {
        return PhoneSubmission.builder()
                .eventId(EVENT_ID_PREFIX + UUID.randomUUID())
                .reservationProcessId(process.getId())
                .catedraProcessId(process.getCatedraProcessId())
                .holdId(process.getHoldId())
                .requestEventId(process.getPhoneRequestEventId())
                .phoneNumber(phoneNumber)
                .createdAt(now)
                .build();
    }

    public void markSent(Instant now) {
        sentAt = now;
    }

    public void recordFailedAttempt() {
        attempts++;
    }
}
