package ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in;

import java.util.UUID;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;

public interface CancelReservationUseCase {

    /** El usuario cancela una reserva propia confirmada (enunciado §5 punto 14). reason es opcional. */
    ReservationProcess cancelReservation(String login, UUID reservationId, String reason);
}
