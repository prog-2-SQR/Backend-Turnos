package ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in;

public interface ReconcileReservationsUseCase {

    /** Compara los procesos en curso con lo que informa la cátedra y los pone al día. Devuelve cuántos cambiaron. */
    int reconcilePendingReservations();
}
