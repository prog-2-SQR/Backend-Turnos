package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Un elemento de GET /api/appointments (anexo §11). Solo los campos que usa la reconciliación; el resto se ignora. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatedraAppointmentResponse {

    private Long reservationId;
    private String reservationProcessId;
    private String status;
}
