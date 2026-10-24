package ar.edu.um.prog2.quiroga.turnos.auth.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import ar.edu.um.prog2.quiroga.turnos.auth.application.exception.AuthException;
import ar.edu.um.prog2.quiroga.turnos.auth.domain.model.AccessToken;
import ar.edu.um.prog2.quiroga.turnos.auth.domain.model.AuthenticatedUser;
import ar.edu.um.prog2.quiroga.turnos.auth.domain.ports.out.CredentialsChecker;
import ar.edu.um.prog2.quiroga.turnos.auth.domain.ports.out.TokenIssuer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserUseCaseImplTest {

    @Mock
    private CredentialsChecker credentialsChecker;

    @Mock
    private TokenIssuer tokenIssuer;

    @InjectMocks
    private AuthenticateUserUseCaseImpl authenticateUserUseCase;

    @Test
    void validCredentialsReturnATokenForThatUser() {
        AuthenticatedUser user = AuthenticatedUser.builder().login("juan.perez").authorities(Set.of("ROLE_USER")).build();
        AccessToken token = AccessToken.builder().value("token").build();
        when(credentialsChecker.check("juan.perez", "una-clave-segura")).thenReturn(user);
        when(tokenIssuer.issue(user, true)).thenReturn(token);

        assertThat(authenticateUserUseCase.authenticate("juan.perez", "una-clave-segura", true)).isSameAs(token);
    }

    @Test
    void invalidCredentialsDoNotIssueAToken() {
        when(credentialsChecker.check("juan.perez", "mala"))
                .thenThrow(new AuthException("Usuario o contraseña incorrectos", null));

        assertThatThrownBy(() -> authenticateUserUseCase.authenticate("juan.perez", "mala", false))
                .isInstanceOf(AuthException.class);
        verify(tokenIssuer, never()).issue(any(), anyBoolean());
    }
}
