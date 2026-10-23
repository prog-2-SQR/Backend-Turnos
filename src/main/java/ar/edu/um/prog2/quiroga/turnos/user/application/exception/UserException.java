package ar.edu.um.prog2.quiroga.turnos.user.application.exception;

import lombok.Getter;

/** Error del registro, con el code que pide el anexo §13.1 para que el cliente decida sin leer el mensaje. */
@Getter
public class UserException extends RuntimeException {

    public static final String USERNAME_ALREADY_EXISTS = "USERNAME_ALREADY_EXISTS";
    public static final String EMAIL_ALREADY_EXISTS = "EMAIL_ALREADY_EXISTS";
    public static final String INVALID_PASSWORD = "INVALID_PASSWORD";

    private final String code;

    public UserException(String code, String message) {
        super(message);
        this.code = code;
    }
}
