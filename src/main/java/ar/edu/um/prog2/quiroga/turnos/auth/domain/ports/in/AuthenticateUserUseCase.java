package ar.edu.um.prog2.quiroga.turnos.auth.domain.ports.in;

import ar.edu.um.prog2.quiroga.turnos.auth.domain.model.AccessToken;

public interface AuthenticateUserUseCase {

    AccessToken authenticate(String login, String password, boolean rememberMe);
}
