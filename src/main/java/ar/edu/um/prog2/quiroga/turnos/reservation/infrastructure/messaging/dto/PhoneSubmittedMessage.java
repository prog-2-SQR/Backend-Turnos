package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.messaging.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AdditionalInformationSubmitted (anexo §15.5). Todos los campos son obligatorios; eventType y schemaVersion tienen que
 * tener exactamente estos valores.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhoneSubmittedMessage {

    public static final String EVENT_TYPE = "AdditionalInformationSubmitted";
    public static final int SCHEMA_VERSION = 1;

    private String eventId;
    private String eventType;
    private String groupId;
    private String reservationProcessId;
    private String holdId;
    private String requestEventId;
    private Information information;
    private Instant occurredAt;
    private Integer schemaVersion;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Information {
        private String phoneNumber;
    }
}
