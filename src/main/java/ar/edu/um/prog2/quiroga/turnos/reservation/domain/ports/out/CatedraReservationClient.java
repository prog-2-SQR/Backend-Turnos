package ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out;

/** Reservas ya confirmadas en la cátedra (anexo §11 y §12). */
public interface CatedraReservationClient {

    /** Cancela la reserva del proceso. Es idempotente: cancelar una ya cancelada no es un error (anexo §12). */
    void cancelReservation(String catedraProcessId, String reason);
}
