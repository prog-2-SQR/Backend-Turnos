package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.web.mapper;

import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.HOLD_EXPIRES_AT;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.MONDAY;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.TWELVE_THIRTY;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.held;
import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationRequest;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.web.dto.ReservationResponse;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.web.dto.StartReservationRequest;
import org.junit.jupiter.api.Test;

class ReservationDtoMapperTest {

    private final ReservationDtoMapper mapper = new ReservationDtoMapper();

    @Test
    void theRequestBecomesTheDomainRequest() {
        ReservationRequest request = mapper.toDomain(StartReservationRequest.builder()
                .professionalId(201L).date(MONDAY).startTime(TWELVE_THIRTY).patientFirstName("Juan").patientLastName("Perez").build());

        assertThat(request.getProfessionalId()).isEqualTo(201L);
        assertThat(request.getDate()).isEqualTo(MONDAY);
        assertThat(request.getStartTime()).isEqualTo(TWELVE_THIRTY);
        assertThat(request.getPatientFirstName()).isEqualTo("Juan");
        assertThat(request.getPatientLastName()).isEqualTo("Perez");
    }

    @Test
    void theResponseShowsTheProcessWithoutTheIdsOfTheCatedra() {
        ReservationProcess process = held();

        ReservationResponse response = mapper.toResponse(process);

        assertThat(response.getId()).isEqualTo(process.getId());
        assertThat(response.getStatus()).isEqualTo("HELD");
        assertThat(response.getProfessionalId()).isEqualTo(201L);
        assertThat(response.getDate()).isEqualTo(MONDAY);
        assertThat(response.getStartTime()).isEqualTo(TWELVE_THIRTY);
        assertThat(response.getExpiresAt()).isEqualTo(HOLD_EXPIRES_AT);
        assertThat(response.getFailureCode()).isNull();
    }

    @Test
    void nullStaysNull() {
        assertThat(mapper.toDomain(null)).isNull();
        assertThat(mapper.toResponse(null)).isNull();
    }
}
