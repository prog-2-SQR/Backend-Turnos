package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.adapter;

import java.util.Arrays;
import java.util.List;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.CatedraReservation;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.CatedraReservationClient;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.dto.CancelReservationCatedraRequest;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.dto.CatedraAppointmentResponse;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.mapper.CatedraAppointmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Reservas en la cátedra, con el JWT técnico: listado de un paciente (anexo §11) y cancelación (anexo §12). Cancelar se
 * reintenta ante timeouts y 5xx: la cátedra lo trata como idempotente, así que repetirlo no tiene efectos de más.
 */
@Component
@RequiredArgsConstructor
public class CatedraReservationClientAdapter implements CatedraReservationClient {

    static final String CANCEL_PATH = "/api/appointments/{reservationProcessId}/cancel";
    // Sin from/to: con fechas la cátedra responde 500 (probado el 02/11). Un paciente tiene pocas reservas.
    static final String APPOINTMENTS_PATH = "/api/appointments?externalPatientId={externalPatientId}&size={size}";
    static final int PAGE_SIZE = 100;

    private final RestClient catedraRestClient;
    private final RetryTemplate catedraRetryTemplate;
    private final CatedraAppointmentMapper catedraAppointmentMapper;

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

    // Leer no tiene efectos: se reintenta ante timeouts y 5xx.
    @Override
    public List<CatedraReservation> findReservationsOfPatient(String externalPatientId) {
        try {
            CatedraAppointmentResponse[] response = catedraRetryTemplate.invoke(() -> catedraRestClient.get()
                    .uri(APPOINTMENTS_PATH, externalPatientId, PAGE_SIZE)
                    .retrieve()
                    .body(CatedraAppointmentResponse[].class));
            return response == null ? List.of() : Arrays.stream(response).map(catedraAppointmentMapper::toDomain).toList();
        } catch (RestClientException e) {
            throw CatedraErrors.translate(e);
        }
    }
}
