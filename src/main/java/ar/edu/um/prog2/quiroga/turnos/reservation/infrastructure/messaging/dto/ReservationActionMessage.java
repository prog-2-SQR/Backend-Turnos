package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.messaging.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un evento del topic de acciones (anexo §15.4 a §15.10). Todos comparten eventId, eventType y reservationProcessId;
 * cada tipo usa además algunos de los otros campos. Los campos que la cátedra agregue en v1 se ignoran (anexo §15.1).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationActionMessage {

    private String eventId;
    private String eventType;
    private String reservationProcessId;
    private String holdId;
    private String requiredField;
    private String message;
    private Instant expiresAt;
    private Instant occurredAt;
    private Integer schemaVersion;
}
