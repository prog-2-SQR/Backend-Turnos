package ar.edu.um.prog2.quiroga.turnos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Si un listener falla, el mensaje se reintenta unas pocas veces con espera: por ejemplo, un evento que llegó antes de
 * que guardáramos la confirmación del hold. Si sigue fallando, se loguea y se confirma su offset para no trabar el
 * topic; la recuperación de procesos pendientes (M7) reconcilia con la cátedra por REST.
 * Spring Boot usa este bean en los listeners que crea.
 */
@Configuration
public class KafkaConfig {

    @Bean
    DefaultErrorHandler kafkaErrorHandler(CatedraProperties catedraProperties) {
        return new DefaultErrorHandler(
                new FixedBackOff(catedraProperties.getRetryDelay().toMillis(), catedraProperties.getMaxRetries()));
    }
}
