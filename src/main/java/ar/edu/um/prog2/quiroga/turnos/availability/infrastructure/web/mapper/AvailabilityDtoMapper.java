package ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.web.mapper;

import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.Availability;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.AvailableSlot;
import ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.web.dto.AvailabilityResponse;
import ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.web.dto.AvailableSlotResponse;
import org.springframework.stereotype.Component;

@Component
public class AvailabilityDtoMapper {

    public AvailabilityResponse toResponse(Availability availability) {
        if (availability == null) return null;
        return AvailabilityResponse.builder()
                .professionalId(availability.getProfessionalId())
                .date(availability.getDate())
                .slots(availability.getSlots().stream().map(this::toResponse).toList())
                .build();
    }

    public AvailableSlotResponse toResponse(AvailableSlot slot) {
        if (slot == null) return null;
        return AvailableSlotResponse.builder()
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .build();
    }
}
