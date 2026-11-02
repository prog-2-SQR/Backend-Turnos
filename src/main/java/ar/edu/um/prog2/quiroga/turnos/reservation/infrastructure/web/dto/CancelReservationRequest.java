package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.web.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body opcional de POST /api/reservations/{id}/cancel. La cátedra acepta un motivo de hasta 500 caracteres (anexo §12). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelReservationRequest {

    @Size(max = 500)
    private String reason;
}
