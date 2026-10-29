package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.web.handler;

import java.util.Map;

import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import ar.edu.um.prog2.quiroga.turnos.shared.web.ApiProblems;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce los errores de la reserva a problem+json. Los codes de la cátedra mantienen su status del anexo §13.1;
 * un code que no se conoce es un rechazo inesperado del servicio central (502).
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReservationExceptionHandler {

    private static final Map<String, HttpStatus> STATUS_BY_CODE = Map.ofEntries(
            Map.entry(ReservationException.RESERVATION_ALREADY_PENDING, HttpStatus.CONFLICT),
            Map.entry(ReservationException.RESERVATION_NOT_FOUND, HttpStatus.NOT_FOUND),
            Map.entry(ReservationException.PATIENT_NAME_REQUIRED, HttpStatus.BAD_REQUEST),
            Map.entry(ReservationException.SLOT_NOT_AVAILABLE, HttpStatus.CONFLICT),
            Map.entry(ReservationException.HOLD_EXPIRED, HttpStatus.CONFLICT),
            Map.entry(ReservationException.CATEDRA_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE),
            Map.entry(ReservationException.CATEDRA_REJECTED, HttpStatus.BAD_GATEWAY),
            Map.entry(ReservationException.INVALID_SLOT, HttpStatus.BAD_REQUEST),
            Map.entry(ReservationException.SLOT_ALREADY_HELD, HttpStatus.CONFLICT),
            Map.entry(ReservationException.SLOT_ALREADY_RESERVED, HttpStatus.CONFLICT),
            Map.entry(ReservationException.PROFESSIONAL_NOT_FOUND, HttpStatus.NOT_FOUND),
            Map.entry(ReservationException.PROFESSIONAL_DISABLED, HttpStatus.BAD_REQUEST),
            Map.entry(ReservationException.INVALID_DATE, HttpStatus.BAD_REQUEST),
            Map.entry(ReservationException.CATALOG_UNAUTHORIZED, HttpStatus.UNAUTHORIZED),
            Map.entry(ReservationException.CATALOG_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE));

    @ExceptionHandler(ReservationException.class)
    public ProblemDetail handleReservationException(ReservationException ex) {
        log.warn("Reserva rechazada: {}", ex.getCode());
        HttpStatus status = STATUS_BY_CODE.getOrDefault(ex.getCode(), HttpStatus.BAD_GATEWAY);
        return ApiProblems.of(status, ex.getCode(), ex.getMessage());
    }
}
