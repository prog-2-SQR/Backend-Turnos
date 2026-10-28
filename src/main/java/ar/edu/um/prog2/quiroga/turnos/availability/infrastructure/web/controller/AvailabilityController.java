package ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.web.controller;

import java.time.LocalDate;

import ar.edu.um.prog2.quiroga.turnos.availability.application.service.AvailabilityService;
import ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.web.dto.AvailabilityResponse;
import ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.web.mapper.AvailabilityDtoMapper;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;
    private final AvailabilityDtoMapper availabilityDtoMapper;

    // Turnos libres de un profesional en una fecha (yyyy-MM-dd, anexo §4). Requiere el JWT del usuario,
    // que Turnos reenvía al catálogo. Falta un parámetro o tiene mal formato → 400 VALIDATION_ERROR.
    @GetMapping
    public AvailabilityResponse getAvailability(
            @RequestParam @Positive Long professionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return availabilityDtoMapper.toResponse(availabilityService.getAvailability(professionalId, date));
    }
}
