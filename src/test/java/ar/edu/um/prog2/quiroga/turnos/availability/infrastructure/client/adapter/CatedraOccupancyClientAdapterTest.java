package ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.client.adapter;

import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;

import ar.edu.um.prog2.quiroga.turnos.availability.application.exception.AvailabilityException;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.ports.out.OccupancyClient;
import ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.client.mapper.OccupancyDtoMapper;
import ar.edu.um.prog2.quiroga.turnos.config.HttpClientsConfig;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.HttpClientErrorException;

class CatedraOccupancyClientAdapterTest {

    private static final String PATH = "/api/appointment-occupancies";
    private static final String JWT = "jwt-tecnico-de-prueba";
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 2);

    // WireMock hace de API de la cátedra.
    @RegisterExtension
    static WireMockExtension catedra = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HttpClientAutoConfiguration.class, RestClientAutoConfiguration.class))
            .withUserConfiguration(HttpClientsConfig.class)
            .withBean(OccupancyDtoMapper.class)
            .withBean(CatedraOccupancyClientAdapter.class)
            .withPropertyValues(
                    "catedra.api-url=" + catedra.baseUrl(),
                    "catedra.jwt=" + JWT,
                    "catedra.retry-delay=10ms",
                    "catalogo.api-url=http://localhost:8080");

    @Test
    void readsTheOccupanciesOfTheDayWithTheTechnicalJwt() {
        // Ejemplo del anexo §8.
        catedra.stubFor(WireMock.get(urlPathEqualTo(PATH)).willReturn(okJson("""
                {"professionalId": 201, "from": "2026-11-02", "to": "2026-11-02", "occupancies": [
                  {"date": "2026-11-02", "startTime": "12:00:00", "status": "CONFIRMED", "expiresAt": null},
                  {"date": "2026-11-02", "startTime": "12:30", "status": "HELD", "expiresAt": "2026-11-01T14:44:50Z"}]}""")));

        contextRunner.run(context -> assertThat(context.getBean(OccupancyClient.class).findOccupancies(201L, MONDAY, MONDAY))
                .extracting("startTime").containsExactly(LocalTime.of(12, 0), LocalTime.of(12, 30)));
        catedra.verify(getRequestedFor(urlPathEqualTo(PATH))
                .withQueryParam("professionalId", equalTo("201"))
                .withQueryParam("from", equalTo("2026-11-02"))
                .withQueryParam("to", equalTo("2026-11-02"))
                .withHeader("Authorization", equalTo("Bearer " + JWT)));
    }

    @Test
    void emptyListMeansNothingIsOccupied() {
        catedra.stubFor(WireMock.get(urlPathEqualTo(PATH)).willReturn(okJson("""
                {"professionalId": 201, "from": "2026-11-02", "to": "2026-11-02", "occupancies": []}""")));

        contextRunner.run(context ->
                assertThat(context.getBean(OccupancyClient.class).findOccupancies(201L, MONDAY, MONDAY)).isEmpty());
    }

    @Test
    void catedraDownAfterTheRetriesIsCatedraUnavailable() {
        catedra.stubFor(WireMock.get(urlPathEqualTo(PATH)).willReturn(serverError()));

        contextRunner.run(context -> assertThatThrownBy(() -> context.getBean(OccupancyClient.class).findOccupancies(201L, MONDAY, MONDAY))
                .isInstanceOf(AvailabilityException.class)
                .extracting("code").isEqualTo(AvailabilityException.CATEDRA_UNAVAILABLE));
        catedra.verify(3, getRequestedFor(urlPathEqualTo(PATH)));
    }

    @Test
    void clientErrorIsATurnosBugAndIsNotTranslated() {
        catedra.stubFor(WireMock.get(urlPathEqualTo(PATH)).willReturn(badRequest()));

        contextRunner.run(context -> assertThatThrownBy(() -> context.getBean(OccupancyClient.class).findOccupancies(201L, MONDAY, MONDAY))
                .isInstanceOf(HttpClientErrorException.BadRequest.class));
        catedra.verify(1, getRequestedFor(urlPathEqualTo(PATH)));
    }
}
