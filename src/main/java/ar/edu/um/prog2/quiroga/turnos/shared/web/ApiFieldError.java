package ar.edu.um.prog2.quiroga.turnos.shared.web;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** Un campo inválido de un body, con el formato de {@code fieldErrors} del anexo §13. */
@Getter
@AllArgsConstructor
public class ApiFieldError {

    private final String objectName;
    private final String field;
    private final String message;
}
