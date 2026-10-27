package ar.edu.um.prog2.quiroga.turnos.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Hash de contraseñas con Argon2id (ADR 0002). Cada intento consume memoria, lo que encarece la fuerza bruta
 * con GPU. Parámetros mínimos de OWASP: 19 MiB de memoria, 2 iteraciones, 1 hilo; sal de 16 bytes, hash de 32.
 */
@Configuration
public class PasswordEncoderConfig {

    static final int SALT_LENGTH = 16;
    static final int HASH_LENGTH = 32;
    static final int PARALLELISM = 1;
    static final int MEMORY_KIB = 19 * 1024;
    static final int ITERATIONS = 2;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(SALT_LENGTH, HASH_LENGTH, PARALLELISM, MEMORY_KIB, ITERATIONS);
    }
}
