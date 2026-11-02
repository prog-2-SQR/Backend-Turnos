package ar.edu.um.prog2.quiroga.turnos.reservation.domain.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un resultado del proceso de reserva que llegó por Kafka (anexo §15.6 a §15.10). Cada tipo usa algunos campos:
 * la confirmación trae el id de la reserva en la cátedra; el rechazo, un mensaje y un nuevo vencimiento; el vencimiento
 * y el proceso inválido, un motivo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResult {

    private String eventId;
    private ReservationResultType type;
    private String catedraProcessId;
    private Long catedraReservationId;
    private String reason;
    private String message;
    private Instant expiresAt;
}
