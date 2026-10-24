package ar.edu.um.prog2.quiroga.turnos.user.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.um.prog2.quiroga.turnos.shared.security.PasswordEncoderConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordEncoderHasherTest {

    private final PasswordEncoder passwordEncoder = new PasswordEncoderConfig().passwordEncoder();
    private final PasswordEncoderHasher hasher = new PasswordEncoderHasher(passwordEncoder);

    @Test
    void hashIsArgon2idWithTheOwaspParametersAndMatchesTheOriginalPassword() {
        String hash = hasher.hash("una-clave-segura");

        assertThat(hash).startsWith("$argon2id$v=19$m=19456,t=2,p=1$").doesNotContain("una-clave-segura");
        assertThat(passwordEncoder.matches("una-clave-segura", hash)).isTrue();
        assertThat(passwordEncoder.matches("otra-clave", hash)).isFalse();
    }

    @Test
    void theSamePasswordGivesDifferentHashes() {
        assertThat(hasher.hash("una-clave-segura")).isNotEqualTo(hasher.hash("una-clave-segura"));
    }

    // Un hash BCrypt viejo ya no se acepta (no hay usuarios reales que migrar).
    @Test
    void aBcryptHashIsNotAccepted() {
        assertThat(passwordEncoder.matches("una-clave-segura",
                "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5BWX4Z3Z0kq0E9tq7Rl1Xc5u5gQ2W")).isFalse();
    }
}
