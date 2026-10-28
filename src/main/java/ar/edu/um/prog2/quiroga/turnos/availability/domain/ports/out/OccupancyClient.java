package ar.edu.um.prog2.quiroga.turnos.availability.domain.ports.out;

import java.time.LocalDate;
import java.util.List;

import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.Occupancy;

/** Turnos ocupados según la cátedra (reservas confirmadas y holds activos), con fechas inclusivas. */
public interface OccupancyClient {

    List<Occupancy> findOccupancies(Long professionalId, LocalDate from, LocalDate to);
}
