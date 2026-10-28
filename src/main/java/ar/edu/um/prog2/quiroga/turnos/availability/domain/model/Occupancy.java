package ar.edu.um.prog2.quiroga.turnos.availability.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Turno no disponible según la cátedra (anexo §8): una reserva confirmada o un hold activo. Para la
 * disponibilidad los dos pesan igual: el turno no se ofrece.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Occupancy {

    private LocalDate date;
    private LocalTime startTime;

    public boolean blocks(AvailableSlot slot) {
        return slot.getDate().equals(date) && slot.getStartTime().equals(startTime);
    }
}
