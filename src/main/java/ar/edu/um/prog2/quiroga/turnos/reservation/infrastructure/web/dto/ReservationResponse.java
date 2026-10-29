package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.web.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Un proceso de reserva visto por la app. Los ids de la cátedra (hold, proceso) quedan del lado del servidor. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponse {

    private UUID id;
    private String status;
    private Long professionalId;
    private LocalDate date;
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;
    private Instant expiresAt;
    private String failureCode;
    /** true: la cátedra ya pidió el teléfono y la app tiene que preguntárselo al usuario. */
    private boolean phoneRequested;
    /** El texto que mandó la cátedra para pedirlo (anexo §15.4). */
    private String phoneRequestMessage;
}
