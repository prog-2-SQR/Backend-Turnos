package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.adapter;

import java.util.List;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.PhoneSubmission;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.PhoneSubmissionOutbox;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.mapper.PhoneSubmissionMapper;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.repository.JpaPhoneSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaPhoneSubmissionOutboxAdapter implements PhoneSubmissionOutbox {

    private final JpaPhoneSubmissionRepository jpaPhoneSubmissionRepository;
    private final PhoneSubmissionMapper phoneSubmissionMapper;

    @Override
    public void savePhoneSubmission(PhoneSubmission submission) {
        jpaPhoneSubmissionRepository.save(phoneSubmissionMapper.toEntity(submission));
    }

    @Override
    public List<PhoneSubmission> findUnsentPhoneSubmissions(int maxAttempts, int limit) {
        return jpaPhoneSubmissionRepository.findBySentAtIsNullAndAttemptsLessThanOrderByCreatedAtAsc(maxAttempts, Limit.of(limit))
                .stream()
                .map(phoneSubmissionMapper::toDomain)
                .toList();
    }
}
