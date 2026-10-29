package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.mapper;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.entity.ReservationProcessEntity;
import org.springframework.stereotype.Component;

@Component
public class ReservationProcessMapper {

    public ReservationProcessEntity toEntity(ReservationProcess domain) {
        if (domain == null) return null;
        return ReservationProcessEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .externalPatientId(domain.getExternalPatientId())
                .professionalId(domain.getProfessionalId())
                .date(domain.getDate())
                .startTime(domain.getStartTime())
                .status(domain.getStatus())
                .holdId(domain.getHoldId())
                .catedraProcessId(domain.getCatedraProcessId())
                .expiresAt(domain.getExpiresAt())
                .failureCode(domain.getFailureCode())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .version(domain.getVersion())
                .build();
    }

    public ReservationProcess toDomain(ReservationProcessEntity entity) {
        if (entity == null) return null;
        return ReservationProcess.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .externalPatientId(entity.getExternalPatientId())
                .professionalId(entity.getProfessionalId())
                .date(entity.getDate())
                .startTime(entity.getStartTime())
                .status(entity.getStatus())
                .holdId(entity.getHoldId())
                .catedraProcessId(entity.getCatedraProcessId())
                .expiresAt(entity.getExpiresAt())
                .failureCode(entity.getFailureCode())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .build();
    }
}
