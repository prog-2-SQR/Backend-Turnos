package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.mapper;

import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.HOLD_EXPIRES_AT;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.MONDAY;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.TWELVE_THIRTY;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.held;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.juan;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.requested;
import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.Hold;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.dto.ConfirmHoldRequest;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.dto.HoldRequest;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.dto.HoldResponse;
import org.junit.jupiter.api.Test;

class HoldDtoMapperTest {

    private final HoldDtoMapper mapper = new HoldDtoMapper();

    @Test
    void theHoldRequestCarriesTheSlotAndTheExternalPatient() {
        HoldRequest request = mapper.toHoldRequest(requested());

        assertThat(request.getProfessionalId()).isEqualTo(201L);
        assertThat(request.getDate()).isEqualTo(MONDAY);
        assertThat(request.getStartTime()).isEqualTo(TWELVE_THIRTY);
        assertThat(request.getExternalPatientId()).isEqualTo("user-42");
    }

    @Test
    void theConfirmationCarriesTheSamePatientAndItsNames() {
        ConfirmHoldRequest request = mapper.toConfirmHoldRequest(held(), juan());

        assertThat(request.getExternalPatientId()).isEqualTo("user-42");
        assertThat(request.getPatientFirstName()).isEqualTo("Juan");
        assertThat(request.getPatientLastName()).isEqualTo("Perez");
    }

    @Test
    void theResponseBecomesAHold() {
        Hold hold = mapper.toDomain(HoldResponse.builder().holdId("hold-1").reservationProcessId("process-1")
                .status("HELD").expiresAt(HOLD_EXPIRES_AT).build());

        assertThat(hold.getHoldId()).isEqualTo("hold-1");
        assertThat(hold.getCatedraProcessId()).isEqualTo("process-1");
        assertThat(hold.getExpiresAt()).isEqualTo(HOLD_EXPIRES_AT);
    }

    @Test
    void nullStaysNull() {
        assertThat(mapper.toHoldRequest(null)).isNull();
        assertThat(mapper.toConfirmHoldRequest(null, juan())).isNull();
        assertThat(mapper.toDomain(null)).isNull();
    }
}
