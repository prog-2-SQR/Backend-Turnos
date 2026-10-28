package ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.client.adapter;

import java.time.LocalDate;
import java.util.List;

import ar.edu.um.prog2.quiroga.turnos.availability.application.exception.AvailabilityException;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.Occupancy;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.ports.out.OccupancyClient;
import ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.client.dto.OccupanciesResponse;
import ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.client.mapper.OccupancyDtoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Ocupaciones de la cátedra (anexo §8) con el JWT técnico. Timeout, sin conexión o 5xx (después de los reintentos)
 * → cátedra no disponible. Un 4xx (rango inválido, JWT técnico rechazado) es un error de Turnos y sigue como 500.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatedraOccupancyClientAdapter implements OccupancyClient {

    static final String OCCUPANCIES_PATH = "/api/appointment-occupancies?professionalId={professionalId}&from={from}&to={to}";

    private final RestClient catedraRestClient;
    private final RetryTemplate catedraRetryTemplate;
    private final OccupancyDtoMapper occupancyDtoMapper;

    @Override
    public List<Occupancy> findOccupancies(Long professionalId, LocalDate from, LocalDate to) {
        try {
            OccupanciesResponse response = catedraRetryTemplate.invoke(() -> catedraRestClient.get()
                    .uri(OCCUPANCIES_PATH, professionalId, from, to)
                    .retrieve()
                    .body(OccupanciesResponse.class));
            return occupancyDtoMapper.toDomain(response);
        } catch (RestClientException e) {
            throw translate(e);
        }
    }

    private static RuntimeException translate(RestClientException e) {
        if (e instanceof HttpClientErrorException) {
            return e;
        }
        log.error("La cátedra no está disponible: {}", e.getMessage());
        return new AvailabilityException(AvailabilityException.CATEDRA_UNAVAILABLE, "La cátedra no está disponible", e);
    }
}
