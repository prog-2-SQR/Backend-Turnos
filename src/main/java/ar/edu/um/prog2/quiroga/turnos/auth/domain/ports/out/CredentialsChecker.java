package ar.edu.um.prog2.quiroga.turnos.auth.domain.ports.out;

import ar.edu.um.prog2.quiroga.turnos.auth.domain.model.AuthenticatedUser;

/** Verifica usuario y contraseña; si no coinciden, lanza AuthException. */
public interface CredentialsChecker {

    AuthenticatedUser check(String login, String password);
}
