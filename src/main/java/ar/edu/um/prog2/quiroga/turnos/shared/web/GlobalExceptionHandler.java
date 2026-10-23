package ar.edu.um.prog2.quiroga.turnos.shared.web;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Errores que no pertenecen a ningún slice. Los errores propios de cada slice (por ejemplo, "el login ya existe")
 * los traduce el handler de ese slice, que tiene prioridad.
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    /** Agrega el {@code code} a los errores estándar de Spring MVC (parámetro inválido, ruta inexistente, etc.). */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        // Spring arma el ProblemDetail dentro de super (a veces body llega en null): el code se agrega después.
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response.getBody() instanceof ProblemDetail problem) {
            problem.setProperty(ApiProblems.CODE, codeFor(statusCode));
        }
        return response;
    }

    /** Body inválido (@Valid): además del code, la lista de campos con error, como pide el anexo §13. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ResponseEntity<Object> response = super.handleMethodArgumentNotValid(ex, headers, status, request);
        if (response.getBody() instanceof ProblemDetail problem) {
            problem.setProperty(ApiProblems.FIELD_ERRORS, fieldErrors(ex));
        }
        return response;
    }

    /** Cualquier error no previsto: 500 sin exponer detalles internos; el detalle queda en el log. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Error no esperado", ex);
        return ApiProblems.of(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR, "Error interno del servidor");
    }

    /** 400 → VALIDATION_ERROR (como el anexo); el resto usa el nombre del estado HTTP (NOT_FOUND, METHOD_NOT_ALLOWED...). */
    private static String codeFor(HttpStatusCode statusCode) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        return status == HttpStatus.BAD_REQUEST ? VALIDATION_ERROR : status.name();
    }

    private static List<ApiFieldError> fieldErrors(MethodArgumentNotValidException ex) {
        return ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toApiFieldError)
                .toList();
    }

    private static ApiFieldError toApiFieldError(FieldError error) {
        return new ApiFieldError(error.getObjectName(), error.getField(), error.getDefaultMessage());
    }
}
