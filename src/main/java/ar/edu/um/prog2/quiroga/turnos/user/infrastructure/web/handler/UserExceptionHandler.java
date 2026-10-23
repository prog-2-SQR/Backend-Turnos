package ar.edu.um.prog2.quiroga.turnos.user.infrastructure.web.handler;

import ar.edu.um.prog2.quiroga.turnos.shared.web.ApiProblems;
import ar.edu.um.prog2.quiroga.turnos.user.application.exception.UserException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserExceptionHandler {

    // Todos los errores del registro son 400 con su code (anexo §13.1).
    @ExceptionHandler(UserException.class)
    public ProblemDetail handleUserException(UserException ex) {
        log.warn("Registro rechazado: {}", ex.getCode());
        return ApiProblems.badRequest(ex.getCode(), ex.getMessage());
    }
}
