package ar.edu.um.prog2.quiroga.turnos.user.infrastructure.security;

import ar.edu.um.prog2.quiroga.turnos.user.application.exception.UserException;
import ar.edu.um.prog2.quiroga.turnos.user.application.service.UserService;
import ar.edu.um.prog2.quiroga.turnos.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Le da a Spring Security los datos de un usuario para validar el login (como DomainUserDetailsService de JHipster).
 * Así el slice auth autentica con el AuthenticationManager de Spring y no necesita conocer al slice user.
 */
@Component
@RequiredArgsConstructor
public class UserDetailsServiceAdapter implements UserDetailsService {

    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String login) {
        try {
            return toUserDetails(userService.findUserByLogin(login));
        } catch (UserException e) {
            throw new UsernameNotFoundException("Usuario no encontrado", e);
        }
    }

    // Un usuario no activado no puede iniciar sesión (Spring lanza DisabledException).
    private static UserDetails toUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.withUsername(user.getLogin())
                .password(user.getPasswordHash())
                .authorities(user.getAuthorities().toArray(String[]::new))
                .disabled(!Boolean.TRUE.equals(user.getActivated()))
                .build();
    }
}
