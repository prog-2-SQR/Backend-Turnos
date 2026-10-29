package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.adapter;

import java.time.Instant;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.ProcessedEventRepository;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.mapper.ProcessedEventMapper;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.repository.JpaProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaProcessedEventRepositoryAdapter implements ProcessedEventRepository {

    private final JpaProcessedEventRepository jpaProcessedEventRepository;
    private final ProcessedEventMapper processedEventMapper;

    @Override
    public boolean existsByEventId(String eventId) {
        return jpaProcessedEventRepository.existsById(eventId);
    }

    @Override
    public void saveProcessedEvent(String eventId, String eventType) {
        jpaProcessedEventRepository.save(processedEventMapper.toEntity(eventId, eventType, Instant.now()));
    }
}
