package ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationResult;

public interface HandleReservationResultUseCase {

    /** Refleja localmente un resultado del proceso que llegó por Kafka (enunciado §5 punto 12). */
    void handleReservationResult(ReservationResult result);
}
