package ar.edu.um.prog2.quiroga.turnos.availability.support;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.Occupancy;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.Professional;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.WeeklySchedule;

/** Datos del snapshot real de la cátedra: Ana Torres (201) atiende los lunes de 12 a 15, turnos de 30 minutos. */
public final class AvailabilityFixtures {

    public static final long ANA_TORRES_ID = 201L;
    public static final LocalDate MONDAY = LocalDate.of(2026, 11, 2);

    private AvailabilityFixtures() {
    }

    public static Professional anaTorres() {
        return Professional.builder().id(ANA_TORRES_ID).categoryId(101L).firstName("Ana").lastName("Torres").enabled(true).build();
    }

    public static WeeklySchedule mondayNoonToThree() {
        return schedule(DayOfWeek.MONDAY, "12:00", "15:00", 30);
    }

    public static WeeklySchedule schedule(DayOfWeek day, String start, String end, int minutes) {
        return WeeklySchedule.builder()
                .id(301L)
                .professionalId(ANA_TORRES_ID)
                .dayOfWeek(day)
                .startTime(LocalTime.parse(start))
                .endTime(LocalTime.parse(end))
                .slotDurationMinutes(minutes)
                .enabled(true)
                .build();
    }

    public static Occupancy occupied(LocalDate date, String start) {
        return Occupancy.builder().date(date).startTime(LocalTime.parse(start)).build();
    }
}
