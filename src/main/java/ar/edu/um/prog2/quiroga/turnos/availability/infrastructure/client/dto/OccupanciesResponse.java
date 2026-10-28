package ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.client.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** JSON de GET /api/appointment-occupancies de la cátedra (anexo §8). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OccupanciesResponse {

    private Long professionalId;
    private LocalDate from;
    private LocalDate to;
    private List<OccupancyResponse> occupancies;
}
