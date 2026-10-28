package ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.web.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityResponse {

    private Long professionalId;
    private LocalDate date;
    private List<AvailableSlotResponse> slots;
}
