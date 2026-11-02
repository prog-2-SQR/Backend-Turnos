package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body de POST /api/appointments/{reservationProcessId}/cancel (anexo §12). reason es opcional, hasta 500 caracteres. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelReservationCatedraRequest {

    private String reason;
}
