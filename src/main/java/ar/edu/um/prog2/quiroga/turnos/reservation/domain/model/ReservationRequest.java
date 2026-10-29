package ar.edu.um.prog2.quiroga.turnos.reservation.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Lo que pide el usuario: un turno (profesional, fecha y hora) y, opcionalmente, los nombres del paciente. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationRequest {

    private Long professionalId;
    private LocalDate date;
    private LocalTime startTime;
    private String patientFirstName;
    private String patientLastName;
}
