package ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Revalida el turno justo antes de pedir el hold (contrato turnos-catalogo): profesional habilitado, horario de su
 * agenda vigente, libre según la cátedra y todavía no empezado.
 */
public interface SlotAvailability {

    boolean isAvailable(Long professionalId, LocalDate date, LocalTime startTime);
}
