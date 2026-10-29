package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.mapper;

import java.time.Instant;

import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.entity.ProcessedEventEntity;
import org.springframework.stereotype.Component;

@Component
public class ProcessedEventMapper {

    public ProcessedEventEntity toEntity(String eventId, String eventType, Instant processedAt) {
        if (eventId == null) return null;
        return ProcessedEventEntity.builder()
                .eventId(eventId)
                .eventType(eventType)
                .processedAt(processedAt)
                .build();
    }
}
