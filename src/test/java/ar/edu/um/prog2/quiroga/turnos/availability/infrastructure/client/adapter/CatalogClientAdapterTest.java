package ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.client.adapter;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable;
import static com.github.tomakehurst.wiremock.client.WireMock.unauthorized;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import ar.edu.um.prog2.quiroga.turnos.availability.application.exception.AvailabilityException;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.Professional;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.WeeklySchedule;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.ports.out.CatalogClient;
import ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.client.mapper.CatalogDtoMapper;
import ar.edu.um.prog2.quiroga.turnos.config.HttpClientsConfig;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.HttpClientErrorException;

class CatalogClientAdapterTest {

    private static final String PROFESSIONAL = "/api/professionals/201";
    private static final String SCHEDULES = "/api/schedules?professionalId=201";

    // WireMock hace de servicio de catálogo.
    @RegisterExtension
    static WireMockExtension catalogo = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HttpClientAutoConfiguration.class, RestClientAutoConfiguration.class))
            .withUserConfiguration(HttpClientsConfig.class)
            .withBean(CatalogDtoMapper.class)
            .withBean(CatalogClientAdapter.class)
            .withPropertyValues(
                    "catedra.api-url=http://localhost:8090",
                    "catedra.jwt=jwt-de-prueba",
                    "catalogo.api-url=" + catalogo.baseUrl(),
                    "catalogo.retry-delay=10ms");

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void findsTheProfessionalWithTheUserToken() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(userJwt()));
        catalogo.stubFor(get(PROFESSIONAL).willReturn(okJson("""
                {"id": 201, "categoryId": 101, "firstName": "Ana", "lastName": "Torres", "enabled": true}""")));

        contextRunner.run(context -> {
            Professional professional = context.getBean(CatalogClient.class).findProfessional(201L).orElseThrow();

            assertThat(professional.getId()).isEqualTo(201L);
            assertThat(professional.getLastName()).isEqualTo("Torres");
            assertThat(professional.isAcceptingAppointments()).isTrue();
        });
        catalogo.verify(getRequestedFor(urlEqualTo(PROFESSIONAL)).withHeader("Authorization", equalTo("Bearer token-del-usuario")));
    }

    @Test
    void unknownProfessionalIsEmpty() {
        catalogo.stubFor(get(PROFESSIONAL).willReturn(notFound()));

        contextRunner.run(context -> assertThat(context.getBean(CatalogClient.class).findProfessional(201L)).isEmpty());
        catalogo.verify(1, getRequestedFor(urlEqualTo(PROFESSIONAL)));
    }

    @Test
    void findsTheWeeklySchedulesAcceptingShortTimes() {
        catalogo.stubFor(get(urlEqualTo(SCHEDULES)).willReturn(okJson("""
                [{"id": 301, "professionalId": 201, "dayOfWeek": "MONDAY", "startTime": "12:00:00",
                  "endTime": "15:00", "slotDurationMinutes": 30, "enabled": true, "campoNuevo": "x"}]""")));

        contextRunner.run(context -> {
            List<WeeklySchedule> schedules = context.getBean(CatalogClient.class).findWeeklySchedules(201L);

            assertThat(schedules).singleElement().satisfies(schedule -> {
                assertThat(schedule.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
                assertThat(schedule.getStartTime()).isEqualTo(LocalTime.of(12, 0));
                assertThat(schedule.getEndTime()).isEqualTo(LocalTime.of(15, 0));
                assertThat(schedule.getSlotDurationMinutes()).isEqualTo(30);
            });
        });
    }

    @Test
    void responseWithoutBodyMeansNoSchedules() {
        // RestClient devuelve null cuando la respuesta no tiene cuerpo: el adapter lo trata como lista vacía.
        catalogo.stubFor(get(urlEqualTo(SCHEDULES)).willReturn(aResponse().withStatus(200)));

        contextRunner.run(context -> assertThat(context.getBean(CatalogClient.class).findWeeklySchedules(201L)).isEmpty());
    }

    @Test
    void catalogDownAfterTheRetriesIsCatalogUnavailable() {
        catalogo.stubFor(get(urlEqualTo(SCHEDULES)).willReturn(serviceUnavailable()));

        contextRunner.run(context -> assertThatThrownBy(() -> context.getBean(CatalogClient.class).findWeeklySchedules(201L))
                .isInstanceOf(AvailabilityException.class)
                .extracting("code").isEqualTo(AvailabilityException.CATALOG_UNAVAILABLE));
        // El primer intento y los 2 reintentos.
        catalogo.verify(3, getRequestedFor(urlEqualTo(SCHEDULES)));
    }

    @Test
    void catalogTooSlowIsCatalogUnavailable() {
        catalogo.stubFor(get(PROFESSIONAL).willReturn(aResponse().withFixedDelay(1_000).withStatus(200)));

        // Timeout corto solo acá: en los demás tests, el primer pedido a WireMock recién levantado puede tardar más.
        contextRunner.withPropertyValues("catalogo.max-retries=0", "catalogo.read-timeout=300ms")
                .run(context -> assertThatThrownBy(() -> context.getBean(CatalogClient.class).findProfessional(201L))
                        .isInstanceOf(AvailabilityException.class)
                        .extracting("code").isEqualTo(AvailabilityException.CATALOG_UNAVAILABLE));
    }

    @Test
    void rejectedTokenIsCatalogUnauthorizedWithoutRetries() {
        catalogo.stubFor(get(PROFESSIONAL).willReturn(unauthorized()));

        contextRunner.run(context -> assertThatThrownBy(() -> context.getBean(CatalogClient.class).findProfessional(201L))
                .isInstanceOf(AvailabilityException.class)
                .extracting("code").isEqualTo(AvailabilityException.CATALOG_UNAUTHORIZED));
        catalogo.verify(1, getRequestedFor(urlEqualTo(PROFESSIONAL)));
    }

    @Test
    void anotherClientErrorIsATurnosBugAndIsNotTranslated() {
        catalogo.stubFor(get(urlEqualTo(SCHEDULES)).willReturn(badRequest()));

        contextRunner.run(context -> assertThatThrownBy(() -> context.getBean(CatalogClient.class).findWeeklySchedules(201L))
                .isInstanceOf(HttpClientErrorException.BadRequest.class));
    }

    private static Jwt userJwt() {
        return Jwt.withTokenValue("token-del-usuario")
                .header("alg", "HS512")
                .subject("juan.perez")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }
}
