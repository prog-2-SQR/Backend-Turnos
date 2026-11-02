package ar.edu.um.prog2.quiroga.turnos.reservation.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una reserva tal como la informa la cátedra en GET /api/appointments (anexo §11). Se usa para reconciliar los procesos
 * locales que quedaron en curso. status es null si la cátedra manda un estado que no conocemos.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatedraReservation {

    private String catedraProcessId;
    private Long catedraReservationId;
    private CatedraReservationStatus status;
}
