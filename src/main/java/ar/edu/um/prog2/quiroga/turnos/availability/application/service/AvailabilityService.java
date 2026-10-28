package ar.edu.um.prog2.quiroga.turnos.availability.application.service;

import java.time.LocalDate;

import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.Availability;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.ports.in.GetAvailabilityUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final GetAvailabilityUseCase getAvailabilityUseCase;

    public Availability getAvailability(Long professionalId, LocalDate date) {
        return getAvailabilityUseCase.getAvailability(professionalId, date);
    }
}
