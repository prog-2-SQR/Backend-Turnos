package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body de POST /api/appointment-holds/{holdId}/confirm (anexo §10): el paciente tiene que ser el mismo del hold. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmHoldRequest {

    private String externalPatientId;
    private String patientFirstName;
    private String patientLastName;
}
