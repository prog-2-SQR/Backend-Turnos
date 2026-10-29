package ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in;

import java.util.Optional;
import java.util.UUID;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;

public interface GetReservationUseCase {

    /** Un proceso del usuario. Si es de otro usuario, no existe para él. */
    Optional<ReservationProcess> getReservation(String login, UUID id);
}
