package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.web.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body de POST /api/reservations. La hora acepta HH:mm o HH:mm:ss (anexo §4). Los nombres son opcionales: si no
 * vienen se usan los de la cuenta; la cátedra los exige de 2 a 100 caracteres (anexo §10).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartReservationRequest {

    @NotNull
    @Positive
    private Long professionalId;
    @NotNull
    private LocalDate date;
    @NotNull
    private LocalTime startTime;
    @Size(min = 2, max = 100)
    private String patientFirstName;
    @Size(min = 2, max = 100)
    private String patientLastName;
}
