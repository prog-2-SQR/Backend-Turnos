package ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.client.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalTime;

import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.Professional;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.WeeklySchedule;
import ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.client.dto.CatalogProfessionalResponse;
import ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.client.dto.CatalogWeeklyScheduleResponse;
import org.junit.jupiter.api.Test;

class CatalogDtoMapperTest {

    private final CatalogDtoMapper mapper = new CatalogDtoMapper();

    @Test
    void mapsTheProfessional() {
        Professional professional = mapper.toDomain(CatalogProfessionalResponse.builder()
                .id(201L).categoryId(101L).firstName("Ana").lastName("Torres").enabled(false).build());

        assertThat(professional.getId()).isEqualTo(201L);
        assertThat(professional.getCategoryId()).isEqualTo(101L);
        assertThat(professional.getFirstName()).isEqualTo("Ana");
        assertThat(professional.getLastName()).isEqualTo("Torres");
        assertThat(professional.getEnabled()).isFalse();
    }

    @Test
    void mapsTheWeeklySchedule() {
        WeeklySchedule schedule = mapper.toDomain(CatalogWeeklyScheduleResponse.builder()
                .id(301L).professionalId(201L).dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(12, 0)).endTime(LocalTime.of(15, 0))
                .slotDurationMinutes(30).enabled(true).build());

        assertThat(schedule.getId()).isEqualTo(301L);
        assertThat(schedule.getProfessionalId()).isEqualTo(201L);
        assertThat(schedule.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(schedule.getStartTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(schedule.getEndTime()).isEqualTo(LocalTime.of(15, 0));
        assertThat(schedule.getSlotDurationMinutes()).isEqualTo(30);
        assertThat(schedule.getEnabled()).isTrue();
    }

    @Test
    void nullStaysNull() {
        assertThat(mapper.toDomain((CatalogProfessionalResponse) null)).isNull();
        assertThat(mapper.toDomain((CatalogWeeklyScheduleResponse) null)).isNull();
    }
}
