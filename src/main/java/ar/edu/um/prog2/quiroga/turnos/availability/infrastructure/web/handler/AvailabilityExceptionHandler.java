package ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.web.handler;

import java.util.Map;

import ar.edu.um.prog2.quiroga.turnos.availability.application.exception.AvailabilityException;
import ar.edu.um.prog2.quiroga.turnos.shared.web.ApiProblems;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce los errores del slice a problem+json, con los status del contrato turnos-catalogo. */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AvailabilityExceptionHandler {

    private static final Map<String, HttpStatus> STATUS_BY_CODE = Map.of(
            AvailabilityException.INVALID_DATE, HttpStatus.BAD_REQUEST,
            AvailabilityException.PROFESSIONAL_NOT_FOUND, HttpStatus.NOT_FOUND,
            AvailabilityException.PROFESSIONAL_DISABLED, HttpStatus.BAD_REQUEST,
            AvailabilityException.CATALOG_UNAUTHORIZED, HttpStatus.UNAUTHORIZED,
            AvailabilityException.CATALOG_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE,
            AvailabilityException.CATEDRA_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE);

    @ExceptionHandler(AvailabilityException.class)
    public ProblemDetail handleAvailabilityException(AvailabilityException ex) {
        log.warn("Consulta de disponibilidad rechazada: {}", ex.getCode());
        HttpStatus status = STATUS_BY_CODE.getOrDefault(ex.getCode(), HttpStatus.INTERNAL_SERVER_ERROR);
        return ApiProblems.of(status, ex.getCode(), ex.getMessage());
    }
}
