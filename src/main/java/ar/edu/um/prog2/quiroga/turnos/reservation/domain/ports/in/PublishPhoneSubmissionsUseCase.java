package ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in;

public interface PublishPhoneSubmissionsUseCase {

    /** Publica los teléfonos del outbox que todavía no se enviaron y tienen menos de maxAttempts intentos fallidos. */
    void publishPendingPhoneSubmissions(int maxAttempts);
}
