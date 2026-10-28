package ar.edu.um.prog2.quiroga.turnos.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// "Ahora" en la zona de las horas de atención. Como bean, los tests pueden usar un reloj fijo.
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${turnos.time-zone}") ZoneId zoneId) {
        return Clock.system(zoneId);
    }
}
