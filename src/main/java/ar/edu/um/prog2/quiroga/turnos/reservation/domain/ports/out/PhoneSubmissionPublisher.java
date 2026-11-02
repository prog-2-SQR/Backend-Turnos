package ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.PhoneSubmission;

/** Envía el teléfono a la cátedra. Si no se puede confirmar el envío, lanza ReservationException (CATEDRA_UNAVAILABLE). */
public interface PhoneSubmissionPublisher {

    void publish(PhoneSubmission submission);
}
