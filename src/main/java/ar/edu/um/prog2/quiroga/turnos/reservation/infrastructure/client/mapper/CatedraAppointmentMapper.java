package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.mapper;

import java.util.Arrays;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.CatedraReservation;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.CatedraReservationStatus;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.dto.CatedraAppointmentResponse;
import org.springframework.stereotype.Component;

@Component
public class CatedraAppointmentMapper {

    // Un estado que no conocemos queda en null: la reconciliación no toca ese proceso.
    public CatedraReservation toDomain(CatedraAppointmentResponse response) {
        if (response == null) return null;
        return CatedraReservation.builder()
                .catedraProcessId(response.getReservationProcessId())
                .catedraReservationId(response.getReservationId())
                .status(Arrays.stream(CatedraReservationStatus.values())
                        .filter(status -> status.name().equals(response.getStatus()))
                        .findFirst().orElse(null))
                .build();
    }
}
