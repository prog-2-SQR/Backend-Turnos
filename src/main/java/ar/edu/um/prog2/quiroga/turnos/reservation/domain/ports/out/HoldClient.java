package ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out;

import java.time.Instant;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.Hold;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.Patient;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;

/** Bloqueo temporal y confirmación inicial en la cátedra (anexo §9 y §10). */
public interface HoldClient {

    Hold createHold(ReservationProcess process);

    /** Confirma el hold con los datos del paciente. Devuelve el vencimiento del proceso, o null si la cátedra no lo informa. */
    Instant confirmHold(ReservationProcess process, Patient patient);
}
