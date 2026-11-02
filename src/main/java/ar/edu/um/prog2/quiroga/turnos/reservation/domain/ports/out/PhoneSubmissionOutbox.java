package ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out;

import java.util.List;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.PhoneSubmission;

/** Teléfonos pendientes de enviar a la cátedra (patrón outbox). */
public interface PhoneSubmissionOutbox {

    void savePhoneSubmission(PhoneSubmission submission);

    /** Los que todavía no se enviaron y tienen menos de maxAttempts intentos fallidos, los más viejos primero. */
    List<PhoneSubmission> findUnsentPhoneSubmissions(int maxAttempts, int limit);
}
