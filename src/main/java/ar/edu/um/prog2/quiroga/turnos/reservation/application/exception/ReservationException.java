package ar.edu.um.prog2.quiroga.turnos.reservation.application.exception;

import lombok.Getter;

/**
 * Error de la reserva, con un code estable (anexo §13): los propios del slice y los que devuelve la cátedra
 * (SLOT_ALREADY_HELD, INVALID_SLOT...), que se pasan tal cual al cliente.
 */
@Getter
public class ReservationException extends RuntimeException {

    public static final String RESERVATION_ALREADY_PENDING = "RESERVATION_ALREADY_PENDING";
    public static final String RESERVATION_NOT_FOUND = "RESERVATION_NOT_FOUND";
    public static final String PATIENT_NAME_REQUIRED = "PATIENT_NAME_REQUIRED";
    public static final String SLOT_NOT_AVAILABLE = "SLOT_NOT_AVAILABLE";
    public static final String HOLD_EXPIRED = "HOLD_EXPIRED";
    public static final String RESERVATION_PROCESS_ALREADY_CONFIRMED = "RESERVATION_PROCESS_ALREADY_CONFIRMED";
    public static final String CATEDRA_UNAVAILABLE = "CATEDRA_UNAVAILABLE";
    public static final String CATEDRA_REJECTED = "CATEDRA_REJECTED";
    /** Un evento llegó antes de que guardáramos la confirmación del hold: el listener lo reintenta. */
    public static final String CONFIRMATION_IN_FLIGHT = "CONFIRMATION_IN_FLIGHT";

    // Codes que llegan de la cátedra (anexo §13.1) o de la disponibilidad, y se pasan tal cual al cliente.
    public static final String INVALID_SLOT = "INVALID_SLOT";
    public static final String SLOT_ALREADY_HELD = "SLOT_ALREADY_HELD";
    public static final String SLOT_ALREADY_RESERVED = "SLOT_ALREADY_RESERVED";
    public static final String PROFESSIONAL_NOT_FOUND = "PROFESSIONAL_NOT_FOUND";
    public static final String PROFESSIONAL_DISABLED = "PROFESSIONAL_DISABLED";
    public static final String INVALID_DATE = "INVALID_DATE";
    public static final String CATALOG_UNAUTHORIZED = "CATALOG_UNAUTHORIZED";
    public static final String CATALOG_UNAVAILABLE = "CATALOG_UNAVAILABLE";

    private final String code;

    public ReservationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public ReservationException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
