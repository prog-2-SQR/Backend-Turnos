package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Respuesta 201 de POST /api/appointment-holds. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HoldResponse {

    private String holdId;
    private String reservationProcessId;
    private String status;
    private Instant expiresAt;
}
