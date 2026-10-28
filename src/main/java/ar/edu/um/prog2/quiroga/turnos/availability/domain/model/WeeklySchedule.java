package ar.edu.um.prog2.quiroga.turnos.availability.domain.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Regla semanal del catálogo: un día de la semana, una franja y la duración de cada turno (anexo §7). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklySchedule {

    private Long id;
    private Long professionalId;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer slotDurationMinutes;
    private Boolean enabled;

    /**
     * Turnos que esta regla genera en una fecha: desde startTime, de a slotDurationMinutes, mientras el turno
     * entero termine antes o justo en endTime. Se cuenta en minutos del día para no pasar de medianoche.
     */
    public List<AvailableSlot> slotsOn(LocalDate date) {
        if (!appliesTo(date)) {
            return List.of();
        }
        List<AvailableSlot> slots = new ArrayList<>();
        int end = minuteOfDay(endTime);
        for (int start = minuteOfDay(startTime); start + slotDurationMinutes <= end; start += slotDurationMinutes) {
            slots.add(AvailableSlot.builder()
                    .date(date)
                    .startTime(LocalTime.MIN.plusMinutes(start))
                    .endTime(LocalTime.MIN.plusMinutes(start + slotDurationMinutes))
                    .build());
        }
        return slots;
    }

    // Habilitada, del mismo día y con una duración que permita armar turnos. DayOfWeek se compara con equals:
    // la documentación de java.time lo declara "value-based" y desaconseja comparar por identidad (==).
    private boolean appliesTo(LocalDate date) {
        return Boolean.TRUE.equals(enabled)
                && date.getDayOfWeek().equals(dayOfWeek)
                && slotDurationMinutes != null && slotDurationMinutes > 0;
    }

    private static int minuteOfDay(LocalTime time) {
        return time.getHour() * 60 + time.getMinute();
    }
}
