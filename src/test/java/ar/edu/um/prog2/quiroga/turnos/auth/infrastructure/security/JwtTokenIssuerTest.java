package ar.edu.um.prog2.quiroga.turnos.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Set;

import javax.crypto.SecretKey;

import ar.edu.um.prog2.quiroga.turnos.TestSecrets;
import ar.edu.um.prog2.quiroga.turnos.auth.domain.model.AccessToken;
import ar.edu.um.prog2.quiroga.turnos.auth.domain.model.AuthenticatedUser;
import ar.edu.um.prog2.quiroga.turnos.shared.security.JwtConfig;
import ar.edu.um.prog2.quiroga.turnos.shared.security.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class JwtTokenIssuerTest {

    private final JwtConfig jwtConfig = new JwtConfig();
    private final JwtProperties properties = new JwtProperties();
    private JwtDecoder decoder;
    private JwtTokenIssuer issuer;

    private final AuthenticatedUser juan = AuthenticatedUser.builder()
            .login("juan.perez").authorities(Set.of("ROLE_USER", "ROLE_ADMIN")).build();

    JwtTokenIssuerTest() {
        properties.setBase64Secret(TestSecrets.JWT_SECRET);
        SecretKey key = jwtConfig.jwtSecretKey(properties);
        decoder = jwtConfig.jwtDecoder(key);
        issuer = new JwtTokenIssuer(jwtConfig.jwtEncoder(key), properties);
    }

    @Test
    void tokenHasTheJhipsterClaimsAndIsValidWithTheSameSecret() {
        AccessToken token = issuer.issue(juan, false);

        Jwt jwt = decoder.decode(token.getValue());
        assertThat(jwt.getSubject()).isEqualTo("juan.perez");
        assertThat(jwt.getClaimAsString(JwtConfig.AUTHORITIES_CLAIM)).isEqualTo("ROLE_ADMIN ROLE_USER");
        assertThat(jwt.getHeaders()).containsEntry("alg", "HS512");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(24));
        assertThat(token.getExpiresAt()).isEqualTo(jwt.getExpiresAt());
    }

    @Test
    void rememberMeLastsThirtyDays() {
        Jwt jwt = decoder.decode(issuer.issue(juan, true).getValue());

        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofDays(30));
    }
}
