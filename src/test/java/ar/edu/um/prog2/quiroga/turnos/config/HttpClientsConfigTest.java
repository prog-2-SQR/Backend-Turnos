package ar.edu.um.prog2.quiroga.turnos.config;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

class HttpClientsConfigTest {

    private static final String CATEDRA_JWT = "jwt-tecnico-de-prueba";
    private static final String USER_TOKEN = "token-del-usuario";

    // Un WireMock hace de cátedra y otro de catálogo.
    @RegisterExtension
    static WireMockExtension catedra = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    @RegisterExtension
    static WireMockExtension catalogo = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HttpClientAutoConfiguration.class, RestClientAutoConfiguration.class))
            .withUserConfiguration(HttpClientsConfig.class)
            .withPropertyValues(
                    "catedra.api-url=" + catedra.baseUrl(),
                    "catedra.jwt=" + CATEDRA_JWT,
                    "catalogo.api-url=" + catalogo.baseUrl(),
                    "catalogo.retry-delay=10ms");

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void catedraClientSendsTheTechnicalJwt() {
        catedra.stubFor(get("/api/ping").willReturn(ok()));

        contextRunner.run(context ->
                context.getBean("catedraRestClient", RestClient.class).get().uri("/api/ping").retrieve().toBodilessEntity());

        catedra.verify(getRequestedFor(urlEqualTo("/api/ping")).withHeader("Authorization", equalTo("Bearer " + CATEDRA_JWT)));
    }

    @Test
    void catalogoClientRelaysTheUserTokenAndNeverTheTechnicalJwt() {
        catalogo.stubFor(get("/api/ping").willReturn(ok()));
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(userJwt()));

        contextRunner.run(context ->
                context.getBean("catalogoRestClient", RestClient.class).get().uri("/api/ping").retrieve().toBodilessEntity());

        catalogo.verify(getRequestedFor(urlEqualTo("/api/ping")).withHeader("Authorization", equalTo("Bearer " + USER_TOKEN)));
    }

    @Test
    void catalogoClientSendsNoTokenWithoutAnAuthenticatedUser() {
        catalogo.stubFor(get("/api/ping").willReturn(ok()));

        contextRunner.run(context ->
                context.getBean("catalogoRestClient", RestClient.class).get().uri("/api/ping").retrieve().toBodilessEntity());

        catalogo.verify(getRequestedFor(urlEqualTo("/api/ping")).withHeader("Authorization", absent()));
    }

    @Test
    void catalogoRetryTemplateRepeatsAfterA5xx() {
        catalogo.stubFor(get("/api/ping").inScenario("caída").whenScenarioStateIs(STARTED)
                .willReturn(serviceUnavailable()).willSetStateTo("recuperado"));
        catalogo.stubFor(get("/api/ping").inScenario("caída").whenScenarioStateIs("recuperado").willReturn(ok()));

        contextRunner.run(context -> {
            RestClient client = context.getBean("catalogoRestClient", RestClient.class);
            RetryTemplate retryTemplate = context.getBean("catalogoRetryTemplate", RetryTemplate.class);

            retryTemplate.invoke(() -> client.get().uri("/api/ping").retrieve().toBodilessEntity());
        });

        catalogo.verify(2, getRequestedFor(urlEqualTo("/api/ping")));
    }

    @Test
    void missingCatalogoUrlStopsTheStartup() {
        contextRunner.withPropertyValues("catalogo.api-url=")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void missingTechnicalJwtStopsTheStartup() {
        contextRunner.withPropertyValues("catedra.jwt=")
                .run(context -> assertThat(context).hasFailed());
    }

    private static Jwt userJwt() {
        return Jwt.withTokenValue(USER_TOKEN)
                .header("alg", "HS512")
                .subject("juan.perez")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }
}
