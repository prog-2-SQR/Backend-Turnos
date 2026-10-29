package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.mapper;

import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.held;
import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import org.junit.jupiter.api.Test;

class ReservationProcessMapperTest {

    private final ReservationProcessMapper mapper = new ReservationProcessMapper();

    @Test
    void aRoundTripKeepsEveryField() {
        ReservationProcess process = held();
        process.setFailureCode("X");
        process.setPhoneRequestEventId("request-event-8a31");
        process.setPhoneRequestMessage("Ingrese un numero");
        process.setVersion(3L);

        ReservationProcess back = mapper.toDomain(mapper.toEntity(process));

        assertThat(back).usingRecursiveComparison().isEqualTo(process);
    }

    @Test
    void nullStaysNull() {
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toDomain(null)).isNull();
    }
}
