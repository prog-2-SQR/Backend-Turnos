package ar.edu.um.prog2.quiroga.turnos.reservation.domain.model;

/** Resultados que manda la cátedra por Kafka (anexo §15.6 a §15.10) y el estado al que llevan el proceso. */
public enum ReservationResultType {
    /** AppointmentConfirmed: el turno quedó reservado. */
    CONFIRMED(ReservationStatus.CONFIRMED),
    /** AdditionalInformationRejected: el teléfono no sirvió; se puede mandar otro hasta expiresAt. */
    PHONE_REJECTED(ReservationStatus.WAITING_FOR_PHONE),
    /** AppointmentProcessExpired. */
    EXPIRED(ReservationStatus.EXPIRED),
    /** AppointmentProcessInvalid: la cátedra detectó una inconsistencia. */
    INVALID(ReservationStatus.INVALID),
    /** AppointmentCancelled: un turno confirmado se canceló. */
    CANCELLED(ReservationStatus.CANCELLED);

    private final ReservationStatus targetStatus;

    ReservationResultType(ReservationStatus targetStatus) {
        this.targetStatus = targetStatus;
    }

    public ReservationStatus getTargetStatus() {
        return targetStatus;
    }
}
