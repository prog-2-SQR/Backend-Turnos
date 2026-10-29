package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Respuesta 202 de la confirmación: status WAITING_FOR_PHONE. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmHoldResponse {

    private String reservationProcessId;
    private String status;
    private Instant expiresAt;
}
