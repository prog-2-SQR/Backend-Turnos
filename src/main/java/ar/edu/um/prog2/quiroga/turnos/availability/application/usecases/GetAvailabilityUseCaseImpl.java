package ar.edu.um.prog2.quiroga.turnos.availability.application.usecases;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import ar.edu.um.prog2.quiroga.turnos.availability.application.exception.AvailabilityException;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.Availability;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.Occupancy;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.Professional;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.WeeklySchedule;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.ports.in.GetAvailabilityUseCase;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.ports.out.CatalogClient;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.ports.out.OccupancyClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Enunciado §7: combina las reglas semanales vigentes del catálogo con las ocupaciones de la cátedra para la fecha
 * y el profesional elegidos. Todo se consulta en el momento (Turnos no replica el catálogo, enunciado §4.2).
 */
@Component
@RequiredArgsConstructor
public class GetAvailabilityUseCaseImpl implements GetAvailabilityUseCase {

    private final CatalogClient catalogClient;
    private final OccupancyClient occupancyClient;
    private final Clock clock;

    @Override
    public Availability getAvailability(Long professionalId, LocalDate date) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (date.isBefore(now.toLocalDate())) {
            throw new AvailabilityException(AvailabilityException.INVALID_DATE, "La fecha ya pasó: " + date);
        }
        Professional professional = catalogClient.findProfessional(professionalId)
                .orElseThrow(() -> new AvailabilityException(AvailabilityException.PROFESSIONAL_NOT_FOUND,
                        "No existe el profesional " + professionalId));
        if (!professional.isAcceptingAppointments()) {
            throw new AvailabilityException(AvailabilityException.PROFESSIONAL_DISABLED,
                    "El profesional " + professionalId + " no está habilitado");
        }
        List<WeeklySchedule> schedules = catalogClient.findWeeklySchedules(professionalId);
        List<Occupancy> occupancies = occupancyClient.findOccupancies(professionalId, date, date);
        return Availability.calculate(professionalId, date, schedules, occupancies, now);
    }
}
