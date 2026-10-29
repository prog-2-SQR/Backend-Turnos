package ar.edu.um.prog2.quiroga.turnos.reservation.domain.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Pedido de información adicional de la cátedra (AdditionalInformationRequested, anexo §15.4). Su eventId se guarda:
 * la respuesta con el teléfono tiene que devolverlo como requestEventId.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhoneRequest {

    private String eventId;
    private String catedraProcessId;
    private String holdId;
    private String requiredField;
    private String message;
    private Instant expiresAt;
}
