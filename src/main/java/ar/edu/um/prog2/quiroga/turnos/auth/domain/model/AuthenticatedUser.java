package ar.edu.um.prog2.quiroga.turnos.auth.domain.model;

import java.util.HashSet;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Quién inició sesión y con qué roles: lo único que auth necesita saber del usuario. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticatedUser {

    private String login;
    @Builder.Default
    private Set<String> authorities = new HashSet<>();
}
