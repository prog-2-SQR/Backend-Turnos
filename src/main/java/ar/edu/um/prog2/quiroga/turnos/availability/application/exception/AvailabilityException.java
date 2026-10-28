package ar.edu.um.prog2.quiroga.turnos.availability.application.exception;

import lombok.Getter;

/** Error de la consulta de disponibilidad, con un code estable para que el cliente decida sin leer el mensaje. */
@Getter
public class AvailabilityException extends RuntimeException {

    public static final String INVALID_DATE = "INVALID_DATE";
    public static final String PROFESSIONAL_NOT_FOUND = "PROFESSIONAL_NOT_FOUND";
    public static final String PROFESSIONAL_DISABLED = "PROFESSIONAL_DISABLED";
    public static final String CATALOG_UNAUTHORIZED = "CATALOG_UNAUTHORIZED";
    public static final String CATALOG_UNAVAILABLE = "CATALOG_UNAVAILABLE";
    public static final String CATEDRA_UNAVAILABLE = "CATEDRA_UNAVAILABLE";

    private final String code;

    public AvailabilityException(String code, String message) {
        super(message);
        this.code = code;
    }

    public AvailabilityException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
