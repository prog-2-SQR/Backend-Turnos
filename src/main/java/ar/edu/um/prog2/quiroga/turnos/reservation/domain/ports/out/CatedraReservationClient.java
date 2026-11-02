package ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out;

import java.util.List;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.CatedraReservation;

/** Reservas en la cátedra: listado (anexo §11) y cancelación (anexo §12). */
public interface CatedraReservationClient {

    /** Cancela la reserva del proceso. Es idempotente: cancelar una ya cancelada no es un error (anexo §12). */
    void cancelReservation(String catedraProcessId, String reason);

    /** Las reservas de un paciente (externalPatientId = user-<id>), para reconciliar los procesos en curso. */
    List<CatedraReservation> findReservationsOfPatient(String externalPatientId);
}
