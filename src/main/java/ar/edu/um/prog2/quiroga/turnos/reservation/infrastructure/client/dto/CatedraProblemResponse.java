package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Lo que interesa de un error de la cátedra (problem+json, anexo §13): se decide por el code, nunca por el texto. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatedraProblemResponse {

    private Integer status;
    private String code;
}
