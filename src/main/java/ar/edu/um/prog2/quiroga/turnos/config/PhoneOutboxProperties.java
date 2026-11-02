package ar.edu.um.prog2.quiroga.turnos.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// Envío del teléfono a la cátedra desde el outbox (anexo §15.2 y §15.5). El groupId de la integración va en cada evento.
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "catedra.phone-outbox")
public class PhoneOutboxProperties {

    @NotBlank
    private String groupId;

    @NotBlank
    private String topic;

    // Cuánto se espera la confirmación de Kafka y cuántos intentos se hacen (anexo §18.4: reintentos limitados).
    @NotNull
    private Duration sendTimeout = Duration.ofSeconds(10);

    @Positive
    private int maxAttempts = 10;
}
