package ar.edu.um.prog2.quiroga.turnos.user.infrastructure.security;

import ar.edu.um.prog2.quiroga.turnos.user.domain.ports.out.PasswordHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Adapter del puerto PasswordHasher sobre el PasswordEncoder de Spring: el algoritmo lo elige la configuración. */
@Component
@RequiredArgsConstructor
public class PasswordEncoderHasher implements PasswordHasher {

    private final PasswordEncoder passwordEncoder;

    @Override
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
