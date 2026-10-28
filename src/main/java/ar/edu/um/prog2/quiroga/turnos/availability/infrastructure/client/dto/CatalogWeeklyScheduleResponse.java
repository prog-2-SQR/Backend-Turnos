package ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.client.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** JSON de GET /api/schedules?professionalId= del catálogo. Las horas llegan como HH:mm:ss o HH:mm (anexo §4). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogWeeklyScheduleResponse {

    private Long id;
    private Long professionalId;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer slotDurationMinutes;
    private Boolean enabled;
}
