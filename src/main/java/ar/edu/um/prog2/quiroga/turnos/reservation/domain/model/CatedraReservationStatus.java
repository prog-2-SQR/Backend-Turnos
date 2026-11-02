package ar.edu.um.prog2.quiroga.turnos.reservation.domain.model;

/** Estados de una reserva en la cátedra (anexo §11) y el estado local que les corresponde. */
public enum CatedraReservationStatus {
    /** Hold confirmado, esperando el teléfono. */
    PHONE_PENDING(ReservationStatus.WAITING_FOR_PHONE),
    CONFIRMED(ReservationStatus.CONFIRMED),
    CANCELLED(ReservationStatus.CANCELLED),
    FAILED(ReservationStatus.FAILED);

    private final ReservationStatus localStatus;

    CatedraReservationStatus(ReservationStatus localStatus) {
        this.localStatus = localStatus;
    }

    public ReservationStatus getLocalStatus() {
        return localStatus;
    }
}
