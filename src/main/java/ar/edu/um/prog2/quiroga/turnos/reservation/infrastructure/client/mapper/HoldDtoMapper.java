package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.mapper;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.Hold;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.Patient;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.dto.ConfirmHoldRequest;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.dto.HoldRequest;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.dto.HoldResponse;
import org.springframework.stereotype.Component;

@Component
public class HoldDtoMapper {

    public HoldRequest toHoldRequest(ReservationProcess process) {
        if (process == null) return null;
        return HoldRequest.builder()
                .professionalId(process.getProfessionalId())
                .date(process.getDate())
                .startTime(process.getStartTime())
                .externalPatientId(process.getExternalPatientId())
                .build();
    }

    public ConfirmHoldRequest toConfirmHoldRequest(ReservationProcess process, Patient patient) {
        if (process == null || patient == null) return null;
        return ConfirmHoldRequest.builder()
                .externalPatientId(process.getExternalPatientId())
                .patientFirstName(patient.getFirstName())
                .patientLastName(patient.getLastName())
                .build();
    }

    public Hold toDomain(HoldResponse response) {
        if (response == null) return null;
        return Hold.builder()
                .holdId(response.getHoldId())
                .catedraProcessId(response.getReservationProcessId())
                .expiresAt(response.getExpiresAt())
                .build();
    }
}
