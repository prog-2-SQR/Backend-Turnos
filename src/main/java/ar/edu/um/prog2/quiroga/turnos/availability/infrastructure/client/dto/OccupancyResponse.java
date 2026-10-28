package ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.client.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Una ocupación: status CONFIRMED o HELD; expiresAt solo en los HELD (anexo §8). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OccupancyResponse {

    private LocalDate date;
    private LocalTime startTime;
    private String status;
    private Instant expiresAt;
}
