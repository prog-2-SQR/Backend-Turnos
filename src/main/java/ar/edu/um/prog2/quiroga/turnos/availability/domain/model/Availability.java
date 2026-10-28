package ar.edu.um.prog2.quiroga.turnos.availability.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Turnos libres de un profesional en una fecha (enunciado §7). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Availability {

    private Long professionalId;
    private LocalDate date;
    private List<AvailableSlot> slots;

    /**
     * Combina las reglas semanales con las ocupaciones de la cátedra: quedan los turnos que genera alguna regla
     * de ese día, que nadie ocupa y que todavía no empezaron. Ordenados por hora.
     */
    public static Availability calculate(Long professionalId, LocalDate date, List<WeeklySchedule> schedules,
                                         List<Occupancy> occupancies, LocalDateTime now) {
        List<AvailableSlot> free = schedules.stream()
                .flatMap(schedule -> schedule.slotsOn(date).stream())
                .filter(slot -> occupancies.stream().noneMatch(occupancy -> occupancy.blocks(slot)))
                .filter(slot -> slot.startsAfter(now))
                .sorted(Comparator.comparing(AvailableSlot::getStartTime))
                .toList();
        return Availability.builder()
                .professionalId(professionalId)
                .date(date)
                .slots(free)
                .build();
    }
}
