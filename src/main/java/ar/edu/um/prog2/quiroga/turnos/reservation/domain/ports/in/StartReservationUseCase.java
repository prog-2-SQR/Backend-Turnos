package ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationRequest;

public interface StartReservationUseCase {

    /** Bloquea el turno en la cátedra y lo confirma inicialmente: el proceso queda esperando el teléfono. */
    ReservationProcess startReservation(String login, ReservationRequest request);
}
