package ar.edu.um.prog2.quiroga.turnos.shared.security;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Firma y validación de los JWT de la app con HS512 y un secreto propio (como JHipster).
 * El mismo secreto firma (login) y valida (cada request protegido).
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    public static final MacAlgorithm ALGORITHM = MacAlgorithm.HS512;
    public static final String AUTHORITIES_CLAIM = "auth";
    // HS512 necesita una clave de al menos 512 bits.
    static final int MIN_SECRET_BYTES = 64;
    static final String INVALID_SECRET =
            "security.jwt.base64-secret tiene que ser Base64 de al menos 64 bytes (openssl rand -base64 64)";

    @Bean
    public SecretKey jwtSecretKey(JwtProperties properties) {
        byte[] secret = decode(properties.getBase64Secret());
        if (secret.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(INVALID_SECRET);
        }
        return new SecretKeySpec(secret, ALGORITHM.getName());
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return NimbusJwtEncoder.withSecretKey(jwtSecretKey).algorithm(ALGORITHM).build();
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(ALGORITHM).build();
    }

    // Los roles viajan en el claim "auth" separados por espacio, ya con el prefijo ROLE_ (formato JHipster).
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(AUTHORITIES_CLAIM);
        authorities.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    private static byte[] decode(String base64Secret) {
        try {
            return Base64.getDecoder().decode(base64Secret);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(INVALID_SECRET, e);
        }
    }
}
