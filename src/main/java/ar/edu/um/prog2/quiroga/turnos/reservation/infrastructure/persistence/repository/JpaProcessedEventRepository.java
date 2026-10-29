package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.repository;

import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.entity.ProcessedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaProcessedEventRepository extends JpaRepository<ProcessedEventEntity, String> {
}
