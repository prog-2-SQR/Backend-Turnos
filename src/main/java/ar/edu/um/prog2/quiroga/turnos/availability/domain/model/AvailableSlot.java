package ar.edu.um.prog2.quiroga.turnos.availability.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Un turno libre: fecha y franja en horas locales de atención (anexo §4). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailableSlot {

    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

    public boolean startsAfter(LocalDateTime moment) {
        return date.atTime(startTime).isAfter(moment);
    }
}
