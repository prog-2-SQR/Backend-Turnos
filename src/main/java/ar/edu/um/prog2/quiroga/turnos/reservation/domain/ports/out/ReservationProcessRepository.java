package ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out;

import java.util.Optional;
import java.util.UUID;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;

public interface ReservationProcessRepository {

    /** Guarda y devuelve el proceso. Un segundo pendiente del mismo usuario se rechaza (RESERVATION_ALREADY_PENDING). */
    ReservationProcess saveReservationProcess(ReservationProcess process);

    Optional<ReservationProcess> findReservationProcessById(UUID id);

    Optional<ReservationProcess> findPendingReservationProcess(Long userId);

    /** Por el reservationProcessId de la cátedra: así llegan identificados los eventos de Kafka (anexo §15). */
    Optional<ReservationProcess> findReservationProcessByCatedraProcessId(String catedraProcessId);
}
