package ar.edu.um.prog2.quiroga.turnos.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import ar.edu.um.prog2.quiroga.turnos.auth.application.exception.AuthException;
import ar.edu.um.prog2.quiroga.turnos.auth.domain.model.AuthenticatedUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class AuthenticationManagerCredentialsCheckerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthenticationManagerCredentialsChecker checker;

    @Test
    void validCredentialsReturnTheLoginAndOnlyTheRoles() {
        when(authenticationManager.authenticate(any())).thenReturn(UsernamePasswordAuthenticationToken.authenticated(
                "juan.perez", null, List.of(new SimpleGrantedAuthority("ROLE_USER"),
                        FactorGrantedAuthority.fromAuthority(FactorGrantedAuthority.PASSWORD_AUTHORITY))));

        AuthenticatedUser user = checker.check("Juan.Perez", "una-clave-segura");

        assertThat(user.getLogin()).isEqualTo("juan.perez");
        assertThat(user.getAuthorities()).containsExactly("ROLE_USER");
    }

    @ParameterizedTest
    @ValueSource(strings = {"bad", "disabled"})
    void anyAuthenticationFailureBecomesTheSameAuthException(String reason) {
        AuthenticationException failure = "bad".equals(reason)
                ? new BadCredentialsException("Bad credentials")
                : new DisabledException("User is disabled");
        when(authenticationManager.authenticate(any())).thenThrow(failure);

        assertThatThrownBy(() -> checker.check("juan.perez", "mala"))
                .isInstanceOf(AuthException.class)
                .hasMessage("Usuario o contraseña incorrectos");
    }
}
