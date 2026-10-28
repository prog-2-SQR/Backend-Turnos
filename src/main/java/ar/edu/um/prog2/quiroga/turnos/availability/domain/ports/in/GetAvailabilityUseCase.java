package ar.edu.um.prog2.quiroga.turnos.availability.domain.ports.in;

import java.time.LocalDate;

import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.Availability;

public interface GetAvailabilityUseCase {

    /** Turnos libres de un profesional habilitado en una fecha de hoy en adelante. */
    Availability getAvailability(Long professionalId, LocalDate date);
}
