package ar.edu.um.prog2.quiroga.turnos.config;

import java.time.Duration;

import ar.edu.um.prog2.quiroga.turnos.shared.security.TokenRelayInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.retry.RetryListener;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryState;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.core.retry.Retryable;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Un RestClient y un RetryTemplate por servicio remoto. Los adapters los reciben por nombre
 * (catedraRestClient, catalogoRestClient...): el nombre del parámetro elige el bean.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({CatedraProperties.class, CatalogoProperties.class})
public class HttpClientsConfig {

    // Cátedra: JWT técnico en cada request (anexo §4).
    @Bean
    public RestClient catedraRestClient(RestClient.Builder builder, CatedraProperties properties) {
        return builder
                .baseUrl(properties.getApiUrl())
                .defaultHeaders(headers -> headers.setBearerAuth(properties.getJwt()))
                .requestFactory(requestFactory(properties.getConnectTimeout(), properties.getReadTimeout()))
                .build();
    }

    // Catálogo: el JWT del usuario que hizo el pedido (token relay), nunca el de la cátedra.
    @Bean
    public RestClient catalogoRestClient(RestClient.Builder builder, CatalogoProperties properties) {
        return builder
                .baseUrl(properties.getApiUrl())
                .requestInterceptor(new TokenRelayInterceptor())
                .requestFactory(requestFactory(properties.getConnectTimeout(), properties.getReadTimeout()))
                .build();
    }

    @Bean
    public RetryTemplate catedraRetryTemplate(CatedraProperties properties) {
        return retryTemplate("la cátedra", properties.getMaxRetries(), properties.getRetryDelay());
    }

    @Bean
    public RetryTemplate catalogoRetryTemplate(CatalogoProperties properties) {
        return retryTemplate("el catálogo", properties.getMaxRetries(), properties.getRetryDelay());
    }

    private static ClientHttpRequestFactory requestFactory(Duration connectTimeout, Duration readTimeout) {
        HttpClientSettings settings = HttpClientSettings.defaults().withTimeouts(connectTimeout, readTimeout);
        return ClientHttpRequestFactoryBuilder.detect().build(settings);
    }

    // Solo se reintenta lo que puede resolverse solo: el servicio no responde (timeout) o falla del lado servidor
    // (5xx). Un 4xx no cambia por repetirlo. La espera se duplica en cada intento.
    private static RetryTemplate retryTemplate(String target, long maxRetries, Duration delay) {
        RetryPolicy policy = RetryPolicy.builder()
                .includes(ResourceAccessException.class, HttpServerErrorException.class)
                .maxRetries(maxRetries)
                .delay(delay)
                .multiplier(2)
                .build();
        RetryTemplate retryTemplate = new RetryTemplate(policy);
        retryTemplate.setRetryListener(new LoggingRetryListener(target));
        return retryTemplate;
    }

    // Deja cada reintento en el log, para que sean observables (anexo §18.4).
    @RequiredArgsConstructor
    static class LoggingRetryListener implements RetryListener {

        private final String target;

        @Override
        public void beforeRetry(RetryPolicy policy, Retryable<?> retryable, RetryState state) {
            log.warn("Reintento {} hacia {} después de: {}",
                    state.getRetryCount(), target, state.getLastException().getMessage());
        }
    }
}
