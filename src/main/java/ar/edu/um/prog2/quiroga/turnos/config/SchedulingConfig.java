package ar.edu.um.prog2.quiroga.turnos.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Habilita los @Scheduled (publicación periódica del outbox de teléfonos).
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
