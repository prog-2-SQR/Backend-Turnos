package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.messaging.mapper;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.PhoneRequest;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.messaging.dto.ReservationActionMessage;
import org.springframework.stereotype.Component;

@Component
public class ReservationActionMapper {

    public PhoneRequest toPhoneRequest(ReservationActionMessage message) {
        if (message == null) return null;
        return PhoneRequest.builder()
                .eventId(message.getEventId())
                .catedraProcessId(message.getReservationProcessId())
                .holdId(message.getHoldId())
                .requiredField(message.getRequiredField())
                .message(message.getMessage())
                .expiresAt(message.getExpiresAt())
                .build();
    }
}
