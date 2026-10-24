package ar.edu.um.prog2.quiroga.turnos.user.infrastructure.web.handler;

import ar.edu.um.prog2.quiroga.turnos.shared.web.ApiProblems;
import ar.edu.um.prog2.quiroga.turnos.user.application.exception.UserException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserExceptionHandler {

    // Los errores del registro son 400 (anexo §13.1); un usuario inexistente, 404. Siempre con su code.
    @ExceptionHandler(UserException.class)
    public ProblemDetail handleUserException(UserException ex) {
        log.warn("Operación de usuario rechazada: {}", ex.getCode());
        return ApiProblems.of(statusOf(ex), ex.getCode(), ex.getMessage());
    }

    private static HttpStatus statusOf(UserException ex) {
        return UserException.USER_NOT_FOUND.equals(ex.getCode()) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
    }
}
