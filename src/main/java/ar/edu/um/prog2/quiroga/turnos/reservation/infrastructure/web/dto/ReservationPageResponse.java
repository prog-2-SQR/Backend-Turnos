package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.web.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Página de GET /api/reservations: el mismo formato que la búsqueda del catálogo. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationPageResponse {

    private List<ReservationResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
