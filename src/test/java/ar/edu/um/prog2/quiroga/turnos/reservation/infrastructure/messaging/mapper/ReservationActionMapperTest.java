package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.messaging.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.PhoneRequest;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationResult;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationResultType;
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
    void theResultKeepsItsTypeAndWhatTheCatedraSent() {
        ReservationResult result = mapper.toResult(ReservationActionMessage.builder()
                .eventId("rejection-event-7d11").eventType("AdditionalInformationRejected")
                .reservationProcessId("process-a92b47d0").reservationId(8251L).reason("INVALID_PHONE_NUMBER")
                .message("El telefono ingresado no es valido.").expiresAt(Instant.parse("2026-09-10T14:44:50Z")).build(),
                ReservationResultType.PHONE_REJECTED);

        assertThat(result.getEventId()).isEqualTo("rejection-event-7d11");
        assertThat(result.getType()).isEqualTo(ReservationResultType.PHONE_REJECTED);
        assertThat(result.getCatedraProcessId()).isEqualTo("process-a92b47d0");
        assertThat(result.getCatedraReservationId()).isEqualTo(8251L);
        assertThat(result.getReason()).isEqualTo("INVALID_PHONE_NUMBER");
        assertThat(result.getMessage()).isEqualTo("El telefono ingresado no es valido.");
        assertThat(result.getExpiresAt()).isEqualTo(Instant.parse("2026-09-10T14:44:50Z"));
    }

    @Test
    void nullStaysNull() {
        assertThat(mapper.toPhoneRequest(null)).isNull();
        assertThat(mapper.toResult(null, ReservationResultType.CONFIRMED)).isNull();
    }
}
