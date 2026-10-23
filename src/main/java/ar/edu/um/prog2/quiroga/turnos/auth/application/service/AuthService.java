package ar.edu.um.prog2.quiroga.turnos.auth.application.service;

import ar.edu.um.prog2.quiroga.turnos.auth.domain.model.AccessToken;
import ar.edu.um.prog2.quiroga.turnos.auth.domain.ports.in.AuthenticateUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticateUserUseCase authenticateUserUseCase;

    public AccessToken authenticate(String login, String password, boolean rememberMe) {
        return authenticateUserUseCase.authenticate(login, password, rememberMe);
    }
}
