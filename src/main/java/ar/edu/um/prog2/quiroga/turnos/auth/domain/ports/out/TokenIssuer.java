package ar.edu.um.prog2.quiroga.turnos.auth.domain.ports.out;

import ar.edu.um.prog2.quiroga.turnos.auth.domain.model.AccessToken;
import ar.edu.um.prog2.quiroga.turnos.auth.domain.model.AuthenticatedUser;

public interface TokenIssuer {

    AccessToken issue(AuthenticatedUser user, boolean rememberMe);
}
