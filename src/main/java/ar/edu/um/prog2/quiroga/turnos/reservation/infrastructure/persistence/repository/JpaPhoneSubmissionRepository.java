package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.repository;

import java.util.List;

import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.entity.PhoneSubmissionEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPhoneSubmissionRepository extends JpaRepository<PhoneSubmissionEntity, String> {

    List<PhoneSubmissionEntity> findBySentAtIsNullAndAttemptsLessThanOrderByCreatedAtAsc(int maxAttempts, Limit limit);
}
