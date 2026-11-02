package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.messaging.mapper;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.PhoneSubmission;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.messaging.dto.PhoneSubmittedMessage;
import org.springframework.stereotype.Component;

@Component
public class PhoneSubmittedMapper {

    // reservationProcessId es el id del proceso en la cátedra, no el nuestro. occurredAt: cuando el usuario lo envió.
    public PhoneSubmittedMessage toMessage(PhoneSubmission submission, String groupId) {
        if (submission == null) return null;
        return PhoneSubmittedMessage.builder()
                .eventId(submission.getEventId())
                .eventType(PhoneSubmittedMessage.EVENT_TYPE)
                .groupId(groupId)
                .reservationProcessId(submission.getCatedraProcessId())
                .holdId(submission.getHoldId())
                .requestEventId(submission.getRequestEventId())
                .information(PhoneSubmittedMessage.Information.builder().phoneNumber(submission.getPhoneNumber()).build())
                .occurredAt(submission.getCreatedAt())
                .schemaVersion(PhoneSubmittedMessage.SCHEMA_VERSION)
                .build();
    }
}
