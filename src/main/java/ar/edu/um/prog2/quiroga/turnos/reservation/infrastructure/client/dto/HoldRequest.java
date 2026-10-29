package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body de POST /api/appointment-holds (anexo §9). La hora va como HH:mm, como en el ejemplo del anexo. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HoldRequest {

    private Long professionalId;
    private LocalDate date;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;
    private String externalPatientId;
}
