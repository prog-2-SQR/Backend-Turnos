package ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.client.mapper;

import java.util.List;

import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.Occupancy;
import ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.client.dto.OccupanciesResponse;
import ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.client.dto.OccupancyResponse;
import org.springframework.stereotype.Component;

@Component
public class OccupancyDtoMapper {

    // Una lista vacía (o ausente) es una respuesta válida: no hay nada ocupado.
    public List<Occupancy> toDomain(OccupanciesResponse response) {
        if (response == null || response.getOccupancies() == null) return List.of();
        return response.getOccupancies().stream()
                .map(this::toDomain)
                .toList();
    }

    public Occupancy toDomain(OccupancyResponse response) {
        if (response == null) return null;
        return Occupancy.builder()
                .date(response.getDate())
                .startTime(response.getStartTime())
                .build();
    }
}
