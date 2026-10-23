package ar.edu.um.prog2.quiroga.turnos.shared.security;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** JWT propio para los usuarios de la app (no es el JWT técnico de la cátedra). Sin toString para no loguear el secreto. */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    @NotBlank
    private String base64Secret;

    // Como JHipster: 24 h, o 30 días con "recordarme".
    @NotNull
    private Duration tokenValidity = Duration.ofHours(24);

    @NotNull
    private Duration tokenValidityRememberMe = Duration.ofDays(30);
}
