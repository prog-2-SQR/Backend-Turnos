package ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.client.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.Occupancy;
import ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.client.dto.OccupanciesResponse;
import ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.client.dto.OccupancyResponse;
import org.junit.jupiter.api.Test;

class OccupancyDtoMapperTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 2);

    private final OccupancyDtoMapper mapper = new OccupancyDtoMapper();

    @Test
    void keepsTheDateAndTheStartOfEachOccupancy() {
        List<Occupancy> occupancies = mapper.toDomain(OccupanciesResponse.builder()
                .occupancies(List.of(OccupancyResponse.builder().date(MONDAY).startTime(LocalTime.of(12, 0)).status("CONFIRMED").build()))
                .build());

        assertThat(occupancies).singleElement().satisfies(occupancy -> {
            assertThat(occupancy.getDate()).isEqualTo(MONDAY);
            assertThat(occupancy.getStartTime()).isEqualTo(LocalTime.of(12, 0));
        });
    }

    @Test
    void missingResponseOrListMeansNothingOccupied() {
        assertThat(mapper.toDomain((OccupanciesResponse) null)).isEmpty();
        assertThat(mapper.toDomain(OccupanciesResponse.builder().build())).isEmpty();
        assertThat(mapper.toDomain((OccupancyResponse) null)).isNull();
    }
}
