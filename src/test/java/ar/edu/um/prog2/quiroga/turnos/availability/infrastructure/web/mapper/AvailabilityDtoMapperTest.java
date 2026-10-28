package ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.web.mapper;

import static ar.edu.um.prog2.quiroga.turnos.availability.support.AvailabilityFixtures.ANA_TORRES_ID;
import static ar.edu.um.prog2.quiroga.turnos.availability.support.AvailabilityFixtures.MONDAY;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;

import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.Availability;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.AvailableSlot;
import ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.web.dto.AvailabilityResponse;
import org.junit.jupiter.api.Test;

class AvailabilityDtoMapperTest {

    private final AvailabilityDtoMapper mapper = new AvailabilityDtoMapper();

    @Test
    void mapsTheDayAndItsSlots() {
        AvailabilityResponse response = mapper.toResponse(Availability.builder()
                .professionalId(ANA_TORRES_ID)
                .date(MONDAY)
                .slots(List.of(AvailableSlot.builder().date(MONDAY).startTime(LocalTime.of(12, 0)).endTime(LocalTime.of(12, 30)).build()))
                .build());

        assertThat(response.getProfessionalId()).isEqualTo(ANA_TORRES_ID);
        assertThat(response.getDate()).isEqualTo(MONDAY);
        assertThat(response.getSlots()).singleElement().satisfies(slot -> {
            assertThat(slot.getStartTime()).isEqualTo(LocalTime.of(12, 0));
            assertThat(slot.getEndTime()).isEqualTo(LocalTime.of(12, 30));
        });
    }

    @Test
    void nullStaysNull() {
        assertThat(mapper.toResponse((Availability) null)).isNull();
        assertThat(mapper.toResponse((AvailableSlot) null)).isNull();
    }
}
