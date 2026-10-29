package ar.edu.um.prog2.quiroga.turnos.reservation.domain.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Bloqueo temporal de la cátedra (anexo §9): su vigencia la marca expiresAt, no una copia local. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hold {

    private String holdId;
    private String catedraProcessId;
    private Instant expiresAt;
}
