package ar.edu.um.prog2.quiroga.turnos.auth.infrastructure.security;

import java.util.Set;
import java.util.stream.Collectors;

import ar.edu.um.prog2.quiroga.turnos.auth.application.exception.AuthException;
import ar.edu.um.prog2.quiroga.turnos.auth.domain.model.AuthenticatedUser;
import ar.edu.um.prog2.quiroga.turnos.auth.domain.ports.out.CredentialsChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.FactorGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationManagerCredentialsChecker implements CredentialsChecker {

    private final AuthenticationManager authenticationManager;

    // Usuario inexistente, contraseña incorrecta o usuario desactivado: el mismo error, para no revelar cuál fue.
    @Override
    public AuthenticatedUser check(String login, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(login, password));
            return AuthenticatedUser.builder()
                    .login(authentication.getName())
                    .authorities(authoritiesOf(authentication))
                    .build();
        } catch (AuthenticationException e) {
            throw new AuthException("Usuario o contraseña incorrectos", e);
        }
    }

    // Solo los roles del usuario. Spring Security 7 agrega además FACTOR_PASSWORD ("entró con contraseña"),
    // que describe este login y no tiene que viajar en el token.
    private static Set<String> authoritiesOf(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .filter(authority -> !(authority instanceof FactorGrantedAuthority))
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
