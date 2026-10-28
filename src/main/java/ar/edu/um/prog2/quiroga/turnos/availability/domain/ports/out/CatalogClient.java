package ar.edu.um.prog2.quiroga.turnos.availability.domain.ports.out;

import java.util.List;
import java.util.Optional;

import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.Professional;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.WeeklySchedule;

/** Información vigente del servicio de catálogo. Turnos no la replica: la consulta en cada operación. */
public interface CatalogClient {

    Optional<Professional> findProfessional(Long professionalId);

    List<WeeklySchedule> findWeeklySchedules(Long professionalId);
}
