package ar.edu.um.prog2.quiroga.turnos.auth.application.usecases;

import ar.edu.um.prog2.quiroga.turnos.auth.domain.model.AccessToken;
import ar.edu.um.prog2.quiroga.turnos.auth.domain.model.AuthenticatedUser;
import ar.edu.um.prog2.quiroga.turnos.auth.domain.ports.in.AuthenticateUserUseCase;
import ar.edu.um.prog2.quiroga.turnos.auth.domain.ports.out.CredentialsChecker;
import ar.edu.um.prog2.quiroga.turnos.auth.domain.ports.out.TokenIssuer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticateUserUseCaseImpl implements AuthenticateUserUseCase {

    private final CredentialsChecker credentialsChecker;
    private final TokenIssuer tokenIssuer;

    @Override
    public AccessToken authenticate(String login, String password, boolean rememberMe) {
        AuthenticatedUser user = credentialsChecker.check(login, password);
        return tokenIssuer.issue(user, rememberMe);
    }
}
