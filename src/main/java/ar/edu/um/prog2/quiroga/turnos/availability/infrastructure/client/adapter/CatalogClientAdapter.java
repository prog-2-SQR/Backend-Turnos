package ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.client.adapter;

import java.util.List;
import java.util.Optional;

import ar.edu.um.prog2.quiroga.turnos.availability.application.exception.AvailabilityException;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.Professional;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.WeeklySchedule;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.ports.out.CatalogClient;
import ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.client.dto.CatalogProfessionalResponse;
import ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.client.dto.CatalogWeeklyScheduleResponse;
import ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.client.mapper.CatalogDtoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Consulta el servicio de catálogo con el token del usuario (token relay, lo agrega el catalogoRestClient).
 * Errores según el contrato turnos-catalogo: 404 → no existe; 401 → token rechazado; timeout, sin conexión o 5xx
 * (después de los reintentos) → catálogo no disponible. Otro 4xx es un error de Turnos y sigue como 500.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogClientAdapter implements CatalogClient {

    static final String PROFESSIONAL_PATH = "/api/professionals/{id}";
    static final String SCHEDULES_PATH = "/api/schedules?professionalId={id}";

    private final RestClient catalogoRestClient;
    private final RetryTemplate catalogoRetryTemplate;
    private final CatalogDtoMapper catalogDtoMapper;

    @Override
    public Optional<Professional> findProfessional(Long professionalId) {
        try {
            CatalogProfessionalResponse response = catalogoRetryTemplate.invoke(() -> catalogoRestClient.get()
                    .uri(PROFESSIONAL_PATH, professionalId)
                    .retrieve()
                    .body(CatalogProfessionalResponse.class));
            return Optional.ofNullable(catalogDtoMapper.toDomain(response));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            throw translate(e);
        }
    }

    @Override
    public List<WeeklySchedule> findWeeklySchedules(Long professionalId) {
        try {
            List<CatalogWeeklyScheduleResponse> response = catalogoRetryTemplate.invoke(() -> catalogoRestClient.get()
                    .uri(SCHEDULES_PATH, professionalId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<CatalogWeeklyScheduleResponse>>() { }));
            return response == null ? List.of() : response.stream().map(catalogDtoMapper::toDomain).toList();
        } catch (RestClientException e) {
            throw translate(e);
        }
    }

    private static RuntimeException translate(RestClientException e) {
        if (e instanceof HttpClientErrorException.Unauthorized) {
            return new AvailabilityException(AvailabilityException.CATALOG_UNAUTHORIZED,
                    "El catálogo rechazó el token del usuario", e);
        }
        if (e instanceof HttpClientErrorException) {
            return e;
        }
        log.error("El catálogo no está disponible: {}", e.getMessage());
        return new AvailabilityException(AvailabilityException.CATALOG_UNAVAILABLE, "El catálogo no está disponible", e);
    }
}
