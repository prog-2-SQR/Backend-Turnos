package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.adapter;

import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.dto.CatedraProblemResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/**
 * Traduce los errores de la API de la cátedra (anexo §13) a ReservationException. Lo usan los adapters del slice.
 * Un 4xx se pasa con su mismo code; timeout, sin conexión o 5xx → CATEDRA_UNAVAILABLE.
 */
@Slf4j
final class CatedraErrors {

    private CatedraErrors() {
    }

    static ReservationException translate(RestClientException e) {
        if (e instanceof HttpClientErrorException clientError) {
            String code = codeOf(clientError);
            log.warn("La cátedra rechazó el pedido: {} {}", clientError.getStatusCode().value(), code);
            return new ReservationException(code, "La cátedra rechazó el pedido (" + code + ")", e);
        }
        log.error("La cátedra no está disponible: {}", e.getMessage());
        return new ReservationException(ReservationException.CATEDRA_UNAVAILABLE, "La cátedra no está disponible", e);
    }

    // Sin cuerpo o sin code (por ejemplo, un 401 de Spring Security) → CATEDRA_REJECTED.
    private static String codeOf(HttpClientErrorException e) {
        try {
            CatedraProblemResponse problem = e.getResponseBodyAs(CatedraProblemResponse.class);
            return problem != null && problem.getCode() != null ? problem.getCode() : ReservationException.CATEDRA_REJECTED;
        } catch (RuntimeException unreadable) {
            return ReservationException.CATEDRA_REJECTED;
        }
    }
}
