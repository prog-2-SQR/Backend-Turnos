package ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.client.mapper;

import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.Professional;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.WeeklySchedule;
import ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.client.dto.CatalogProfessionalResponse;
import ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.client.dto.CatalogWeeklyScheduleResponse;
import org.springframework.stereotype.Component;

@Component
public class CatalogDtoMapper {

    public Professional toDomain(CatalogProfessionalResponse response) {
        if (response == null) return null;
        return Professional.builder()
                .id(response.getId())
                .categoryId(response.getCategoryId())
                .firstName(response.getFirstName())
                .lastName(response.getLastName())
                .enabled(response.getEnabled())
                .build();
    }

    public WeeklySchedule toDomain(CatalogWeeklyScheduleResponse response) {
        if (response == null) return null;
        return WeeklySchedule.builder()
                .id(response.getId())
                .professionalId(response.getProfessionalId())
                .dayOfWeek(response.getDayOfWeek())
                .startTime(response.getStartTime())
                .endTime(response.getEndTime())
                .slotDurationMinutes(response.getSlotDurationMinutes())
                .enabled(response.getEnabled())
                .build();
    }
}
