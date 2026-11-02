package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.mapper;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.PhoneSubmission;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.entity.PhoneSubmissionEntity;
import org.springframework.stereotype.Component;

@Component
public class PhoneSubmissionMapper {

    public PhoneSubmissionEntity toEntity(PhoneSubmission domain) {
        if (domain == null) return null;
        return PhoneSubmissionEntity.builder()
                .eventId(domain.getEventId())
                .reservationProcessId(domain.getReservationProcessId())
                .catedraProcessId(domain.getCatedraProcessId())
                .holdId(domain.getHoldId())
                .requestEventId(domain.getRequestEventId())
                .phoneNumber(domain.getPhoneNumber())
                .createdAt(domain.getCreatedAt())
                .sentAt(domain.getSentAt())
                .attempts(domain.getAttempts())
                .build();
    }

    public PhoneSubmission toDomain(PhoneSubmissionEntity entity) {
        if (entity == null) return null;
        return PhoneSubmission.builder()
                .eventId(entity.getEventId())
                .reservationProcessId(entity.getReservationProcessId())
                .catedraProcessId(entity.getCatedraProcessId())
                .holdId(entity.getHoldId())
                .requestEventId(entity.getRequestEventId())
                .phoneNumber(entity.getPhoneNumber())
                .createdAt(entity.getCreatedAt())
                .sentAt(entity.getSentAt())
                .attempts(entity.getAttempts())
                .build();
    }
}
