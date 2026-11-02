package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body de POST /api/reservations/{id}/phone. Se acepta con espacios, guiones o paréntesis; la regla la aplica el caso de uso. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitPhoneRequest {

    @NotBlank
    @Size(max = 30)
    private String phoneNumber;
}
