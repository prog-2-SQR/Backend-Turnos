package ar.edu.um.prog2.quiroga.turnos.auth.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import ar.edu.um.prog2.quiroga.turnos.auth.domain.model.AccessToken;
import ar.edu.um.prog2.quiroga.turnos.auth.domain.ports.in.AuthenticateUserUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticateUserUseCase authenticateUserUseCase;

    @InjectMocks
    private AuthService authService;

    @Test
    void authenticateDelegatesToTheUseCase() {
        AccessToken token = AccessToken.builder().value("token").build();
        when(authenticateUserUseCase.authenticate("juan.perez", "clave", false)).thenReturn(token);

        assertThat(authService.authenticate("juan.perez", "clave", false)).isSameAs(token);
    }
}
