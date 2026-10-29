package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.adapter;

import java.time.Instant;

import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.Hold;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.Patient;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.HoldClient;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.dto.CatedraProblemResponse;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.dto.ConfirmHoldResponse;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.dto.HoldResponse;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.mapper.HoldDtoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Hold y confirmación en la cátedra, con el JWT técnico (anexo §9 y §10).
 * - Crear el hold NO se reintenta: repetirlo chocaría con nuestro propio hold (SLOT_ALREADY_HELD) y no se podría
 *   recuperar su holdId. Si se pierde la respuesta, ese hold vence solo en la cátedra.
 * - Confirmar SÍ se reintenta (timeouts y 5xx): si ya estaba confirmado, la cátedra responde
 *   RESERVATION_PROCESS_ALREADY_CONFIRMED y eso cuenta como éxito (anexo §18.4: reconciliar antes de repetir).
 * Un 4xx de la cátedra se traduce con su mismo code; timeout, sin conexión o 5xx → CATEDRA_UNAVAILABLE.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatedraHoldClientAdapter implements HoldClient {

    static final String HOLDS_PATH = "/api/appointment-holds";
    static final String CONFIRM_PATH = "/api/appointment-holds/{holdId}/confirm";

    private final RestClient catedraRestClient;
    private final RetryTemplate catedraRetryTemplate;
    private final HoldDtoMapper holdDtoMapper;

    @Override
    public Hold createHold(ReservationProcess process) {
        try {
            HoldResponse response = catedraRestClient.post()
                    .uri(HOLDS_PATH)
                    .body(holdDtoMapper.toHoldRequest(process))
                    .retrieve()
                    .body(HoldResponse.class);
            if (response == null) {
                throw new ReservationException(ReservationException.CATEDRA_REJECTED, "La cátedra respondió el hold sin contenido");
            }
            return holdDtoMapper.toDomain(response);
        } catch (RestClientException e) {
            throw translate(e);
        }
    }

    @Override
    public Instant confirmHold(ReservationProcess process, Patient patient) {
        try {
            ConfirmHoldResponse response = catedraRetryTemplate.invoke(() -> catedraRestClient.post()
                    .uri(CONFIRM_PATH, process.getHoldId())
                    .body(holdDtoMapper.toConfirmHoldRequest(process, patient))
                    .retrieve()
                    .body(ConfirmHoldResponse.class));
            return response == null ? null : response.getExpiresAt();
        } catch (RestClientException e) {
            ReservationException error = translate(e);
            if (ReservationException.RESERVATION_PROCESS_ALREADY_CONFIRMED.equals(error.getCode())) {
                log.info("El hold {} ya estaba confirmado: se toma como confirmado", process.getHoldId());
                return null;
            }
            throw error;
        }
    }

    private static ReservationException translate(RestClientException e) {
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
