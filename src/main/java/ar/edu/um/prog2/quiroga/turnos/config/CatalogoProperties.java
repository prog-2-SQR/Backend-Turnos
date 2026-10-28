package ar.edu.um.prog2.quiroga.turnos.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// Acceso al servicio de catálogo. Valores por defecto del contrato entre servicios (timeouts y reintentos).
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "catalogo")
public class CatalogoProperties {

    @NotBlank
    private String apiUrl;

    @NotNull
    private Duration connectTimeout = Duration.ofSeconds(2);

    @NotNull
    private Duration readTimeout = Duration.ofSeconds(5);

    // Solo se repiten lecturas (GET): no tienen efectos.
    @PositiveOrZero
    private long maxRetries = 2;

    @NotNull
    private Duration retryDelay = Duration.ofMillis(500);
}
