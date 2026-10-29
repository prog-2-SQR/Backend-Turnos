package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.availability;

import java.time.LocalDate;
import java.time.LocalTime;

import ar.edu.um.prog2.quiroga.turnos.availability.application.exception.AvailabilityException;
import ar.edu.um.prog2.quiroga.turnos.availability.application.service.AvailabilityService;
import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.SlotAvailability;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Único punto de contacto de reservation con el slice availability (ADR 0004, puerto de salida): usa su service,
 * nunca su dominio ni su infraestructura. Los errores de la disponibilidad pasan con el mismo code.
 */
@Component
@RequiredArgsConstructor
public class AvailabilitySlotAdapter implements SlotAvailability {

    private final AvailabilityService availabilityService;

    @Override
    public boolean isAvailable(Long professionalId, LocalDate date, LocalTime startTime) {
        try {
            return availabilityService.getAvailability(professionalId, date).getSlots().stream()
                    .anyMatch(slot -> slot.getStartTime().equals(startTime));
        } catch (AvailabilityException e) {
            throw new ReservationException(e.getCode(), e.getMessage(), e);
        }
    }
}
