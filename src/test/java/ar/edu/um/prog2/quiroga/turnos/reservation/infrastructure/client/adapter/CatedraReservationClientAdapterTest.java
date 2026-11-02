package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.adapter;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.um.prog2.quiroga.turnos.config.HttpClientsConfig;
import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.CatedraReservationClient;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CatedraReservationClientAdapterTest {

    private static final String CANCEL = "/api/appointments/process-a92b47d0/cancel";
    private static final String CANCELLED = """
            {"reservationId": 8251, "reservationProcessId": "process-a92b47d0", "status": "CANCELLED",
             "cancelledAt": "2026-09-11T15:10:00Z"}""";

    // WireMock hace de API de la cátedra.
    @RegisterExtension
    static WireMockExtension catedra = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HttpClientAutoConfiguration.class, RestClientAutoConfiguration.class))
            .withUserConfiguration(HttpClientsConfig.class)
            .withBean(CatedraReservationClientAdapter.class)
            .withPropertyValues(
                    "catedra.api-url=" + catedra.baseUrl(),
                    "catedra.jwt=jwt-tecnico-de-prueba",
                    "catedra.retry-delay=10ms",
                    "catalogo.api-url=http://localhost:8080");

    @Test
    void cancelsWithTheReasonAndTheTechnicalJwt() {
        // Ejemplo del anexo §12.
        catedra.stubFor(post(CANCEL).willReturn(okJson(CANCELLED)));

        contextRunner.run(context -> context.getBean(CatedraReservationClient.class)
                .cancelReservation("process-a92b47d0", "Cambio solicitado por el usuario"));

        catedra.verify(postRequestedFor(urlEqualTo(CANCEL))
                .withHeader("Authorization", equalTo("Bearer jwt-tecnico-de-prueba"))
                .withRequestBody(equalToJson("{\"reason\": \"Cambio solicitado por el usuario\"}")));
    }

    @Test
    void aFallingCatedraIsRetriedBecauseCancellingIsIdempotent() {
        catedra.stubFor(post(CANCEL).inScenario("caída").whenScenarioStateIs(STARTED)
                .willReturn(serviceUnavailable()).willSetStateTo("de vuelta"));
        catedra.stubFor(post(CANCEL).inScenario("caída").whenScenarioStateIs("de vuelta").willReturn(okJson(CANCELLED)));

        contextRunner.run(context -> context.getBean(CatedraReservationClient.class).cancelReservation("process-a92b47d0", null));

        catedra.verify(2, postRequestedFor(urlEqualTo(CANCEL)));
    }

    @Test
    void aReservationThatIsNotCancellableKeepsTheCodeOfTheCatedra() {
        catedra.stubFor(post(CANCEL).willReturn(aResponse().withStatus(409).withHeader("Content-Type", "application/problem+json")
                .withBody("{\"status\": 409, \"code\": \"RESERVATION_NOT_CANCELLABLE\"}")));

        contextRunner.run(context -> assertThatThrownBy(() -> context.getBean(CatedraReservationClient.class)
                .cancelReservation("process-a92b47d0", null))
                .isInstanceOf(ReservationException.class)
                .extracting("code").isEqualTo("RESERVATION_NOT_CANCELLABLE"));
    }

    @Test
    void aCatedraThatStaysDownIsCatedraUnavailable() {
        catedra.stubFor(post(CANCEL).willReturn(serviceUnavailable()));

        contextRunner.run(context -> assertThatThrownBy(() -> context.getBean(CatedraReservationClient.class)
                .cancelReservation("process-a92b47d0", null))
                .isInstanceOf(ReservationException.class)
                .extracting("code").isEqualTo(ReservationException.CATEDRA_UNAVAILABLE));
    }
}
