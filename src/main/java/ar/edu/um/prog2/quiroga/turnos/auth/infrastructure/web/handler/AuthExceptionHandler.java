package ar.edu.um.prog2.quiroga.turnos.auth.infrastructure.web.handler;

import ar.edu.um.prog2.quiroga.turnos.auth.application.exception.AuthException;
import ar.edu.um.prog2.quiroga.turnos.shared.web.ApiProblems;
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
public class AuthExceptionHandler {

    static final String BAD_CREDENTIALS = "BAD_CREDENTIALS";

    @ExceptionHandler(AuthException.class)
    public ProblemDetail handleAuthException(AuthException ex) {
        log.warn("Login rechazado: {}", ex.getCause().getClass().getSimpleName());
        return ApiProblems.of(HttpStatus.UNAUTHORIZED, BAD_CREDENTIALS, ex.getMessage());
    }
}
