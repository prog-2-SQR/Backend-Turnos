package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.messaging.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.PhoneRequest;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.messaging.dto.ReservationActionMessage;
import org.junit.jupiter.api.Test;

class ReservationActionMapperTest {

    private final ReservationActionMapper mapper = new ReservationActionMapper();

    @Test
    void thePhoneRequestKeepsTheEventAndTheProcessOfTheCatedra() {
        PhoneRequest request = mapper.toPhoneRequest(ReservationActionMessage.builder()
                .eventId("request-event-8a31").eventType("AdditionalInformationRequested")
                .reservationProcessId("process-a92b47d0").holdId("hold-6c0c2f41").requiredField("PHONE_NUMBER")
                .message("Ingrese un numero").expiresAt(Instant.parse("2026-09-10T14:44:50Z")).build());

        assertThat(request.getEventId()).isEqualTo("request-event-8a31");
        assertThat(request.getCatedraProcessId()).isEqualTo("process-a92b47d0");
        assertThat(request.getHoldId()).isEqualTo("hold-6c0c2f41");
        assertThat(request.getRequiredField()).isEqualTo("PHONE_NUMBER");
        assertThat(request.getMessage()).isEqualTo("Ingrese un numero");
        assertThat(request.getExpiresAt()).isEqualTo(Instant.parse("2026-09-10T14:44:50Z"));
    }

    @Test
    void nullStaysNull() {
        assertThat(mapper.toPhoneRequest(null)).isNull();
    }
}
