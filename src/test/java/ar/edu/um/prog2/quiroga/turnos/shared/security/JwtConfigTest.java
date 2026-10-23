package ar.edu.um.prog2.quiroga.turnos.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtConfigTest {

    private final JwtConfig jwtConfig = new JwtConfig();

    @Test
    void rejectsASecretShorterThan64Bytes() {
        JwtProperties properties = new JwtProperties();
        properties.setBase64Secret(Base64.getEncoder().encodeToString(new byte[32]));

        assertThatThrownBy(() -> jwtConfig.jwtSecretKey(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("al menos 64 bytes");
    }

    @Test
    void rejectsASecretThatIsNotBase64() {
        JwtProperties properties = new JwtProperties();
        properties.setBase64Secret("cambiar-por-un-secreto-propio");

        assertThatThrownBy(() -> jwtConfig.jwtSecretKey(properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Base64");
    }

    // Spring Security 7 agrega además FACTOR_BEARER (indica que se autenticó con un token).
    @Test
    void rolesComeFromTheAuthClaimWithoutAddingAPrefix() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS512").subject("juan.perez")
                .claim("auth", "ROLE_USER ROLE_ADMIN").issuedAt(Instant.now()).build();

        assertThat(jwtConfig.jwtAuthenticationConverter().convert(jwt).getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_USER", "ROLE_ADMIN")
                .doesNotContain("SCOPE_ROLE_USER");
    }
}
