package ar.edu.um.prog2.quiroga.turnos.user.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.um.prog2.quiroga.turnos.shared.security.PasswordEncoderConfig;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class BCryptPasswordHasherTest {

    private final PasswordEncoder passwordEncoder = new PasswordEncoderConfig().passwordEncoder();
    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher(passwordEncoder);

    @Test
    void hashIsBCryptAndMatchesTheOriginalPassword() {
        String hash = hasher.hash("una-clave-segura");

        assertThat(hash).startsWith("$2a$").hasSize(60).doesNotContain("una-clave-segura");
        assertThat(passwordEncoder.matches("una-clave-segura", hash)).isTrue();
        assertThat(passwordEncoder.matches("otra-clave", hash)).isFalse();
    }

    @Test
    void theSamePasswordGivesDifferentHashes() {
        assertThat(hasher.hash("una-clave-segura")).isNotEqualTo(hasher.hash("una-clave-segura"));
    }
}
