package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.web.mapper;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationPage;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationRequest;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.web.dto.ReservationPageResponse;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.web.dto.ReservationResponse;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.web.dto.StartReservationRequest;
import org.springframework.stereotype.Component;

@Component
public class ReservationDtoMapper {

    public ReservationRequest toDomain(StartReservationRequest request) {
        if (request == null) return null;
        return ReservationRequest.builder()
                .professionalId(request.getProfessionalId())
                .date(request.getDate())
                .startTime(request.getStartTime())
                .patientFirstName(request.getPatientFirstName())
                .patientLastName(request.getPatientLastName())
                .build();
    }

    public ReservationResponse toResponse(ReservationProcess process) {
        if (process == null) return null;
        return ReservationResponse.builder()
                .id(process.getId())
                .status(process.getStatus().name())
                .professionalId(process.getProfessionalId())
                .date(process.getDate())
                .startTime(process.getStartTime())
                .expiresAt(process.getExpiresAt())
                .failureCode(process.getFailureCode())
                .phoneRequested(process.isPhoneRequested())
                .phoneRequestMessage(process.isPhoneRequested() ? process.getPhoneRequestMessage() : null)
                .phoneRejectionMessage(process.isPhoneRequested() ? process.getPhoneRejectionMessage() : null)
                .build();
    }

    public ReservationPageResponse toPageResponse(ReservationPage page) {
        if (page == null) return null;
        return ReservationPageResponse.builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .page(page.getPage())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.totalPages())
                .build();
    }
}
