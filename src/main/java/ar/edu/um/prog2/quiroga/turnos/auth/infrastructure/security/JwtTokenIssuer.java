package ar.edu.um.prog2.quiroga.turnos.auth.infrastructure.security;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import ar.edu.um.prog2.quiroga.turnos.auth.domain.model.AccessToken;
import ar.edu.um.prog2.quiroga.turnos.auth.domain.model.AuthenticatedUser;
import ar.edu.um.prog2.quiroga.turnos.auth.domain.ports.out.TokenIssuer;
import ar.edu.um.prog2.quiroga.turnos.shared.security.JwtConfig;
import ar.edu.um.prog2.quiroga.turnos.shared.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/** JWT con el formato de JHipster: sub = login, auth = roles separados por espacio, iat y exp. */
@Component
@RequiredArgsConstructor
public class JwtTokenIssuer implements TokenIssuer {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    @Override
    public AccessToken issue(AuthenticatedUser user, boolean rememberMe) {
        // El JWT guarda las fechas en segundos enteros.
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = now.plus(validity(rememberMe));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getLogin())
                .claim(JwtConfig.AUTHORITIES_CLAIM, String.join(" ", user.getAuthorities().stream().sorted().toList()))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(JwtConfig.ALGORITHM).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return AccessToken.builder().value(token).expiresAt(expiresAt).build();
    }

    private Duration validity(boolean rememberMe) {
        return rememberMe ? jwtProperties.getTokenValidityRememberMe() : jwtProperties.getTokenValidity();
    }
}
