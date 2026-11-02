package ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationPage;

public interface ListReservationsUseCase {

    /** Las reservas del usuario, de la más nueva a la más vieja (enunciado §5 punto 13). */
    ReservationPage listReservations(String login, int page, int size);
}
