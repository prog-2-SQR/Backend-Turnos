package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.adapter;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.CatedraReservationClient;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.dto.CancelReservationCatedraRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Reservas confirmadas en la cátedra, con el JWT técnico. Cancelar se reintenta ante timeouts y 5xx: la cátedra lo
 * trata como idempotente (anexo §12), así que repetirlo no tiene efectos de más.
 */
@Component
@RequiredArgsConstructor
public class CatedraReservationClientAdapter implements CatedraReservationClient {

    static final String CANCEL_PATH = "/api/appointments/{reservationProcessId}/cancel";

    private final RestClient catedraRestClient;
    private final RetryTemplate catedraRetryTemplate;

    @Override
    public void cancelReservation(String catedraProcessId, String reason) {
        try {
            catedraRetryTemplate.invoke(() -> catedraRestClient.post()
                    .uri(CANCEL_PATH, catedraProcessId)
                    .body(CancelReservationCatedraRequest.builder().reason(reason).build())
                    .retrieve()
                    .toBodilessEntity());
        } catch (RestClientException e) {
            throw CatedraErrors.translate(e);
        }
    }
}
