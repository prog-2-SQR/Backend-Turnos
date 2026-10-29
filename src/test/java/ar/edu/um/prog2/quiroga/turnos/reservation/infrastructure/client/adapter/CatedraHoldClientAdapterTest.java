package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.adapter;

import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.HOLD_EXPIRES_AT;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.held;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.juan;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.requested;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import ar.edu.um.prog2.quiroga.turnos.config.HttpClientsConfig;
import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.Hold;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.HoldClient;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.client.mapper.HoldDtoMapper;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class CatedraHoldClientAdapterTest {

    private static final String HOLDS = "/api/appointment-holds";
    private static final String CONFIRM = "/api/appointment-holds/hold-6c0c2f41/confirm";
    private static final String JWT = "jwt-tecnico-de-prueba";

    // WireMock hace de API de la cátedra.
    @RegisterExtension
    static WireMockExtension catedra = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HttpClientAutoConfiguration.class, RestClientAutoConfiguration.class))
            .withUserConfiguration(HttpClientsConfig.class)
            .withBean(HoldDtoMapper.class)
            .withBean(CatedraHoldClientAdapter.class)
            .withPropertyValues(
                    "catedra.api-url=" + catedra.baseUrl(),
                    "catedra.jwt=" + JWT,
                    "catedra.retry-delay=10ms",
                    "catalogo.api-url=http://localhost:8080");

    private static ResponseDefinitionBuilder problem(int status, String code) {
        return aResponse().withStatus(status).withHeader("Content-Type", "application/problem+json")
                .withBody("{\"status\": " + status + ", \"code\": \"" + code + "\", \"detail\": \"texto de la cátedra\"}");
    }

    @Test
    void createsTheHoldWithTheSlotAndTheExternalPatient() {
        // Ejemplo del anexo §9.
        catedra.stubFor(post(HOLDS).willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                .withBody("{\"holdId\": \"hold-6c0c2f41\", \"reservationProcessId\": \"process-a92b47d0\", \"status\": \"HELD\","
                        + " \"expiresAt\": \"2026-10-29T15:10:00Z\"}")));

        contextRunner.run(context -> {
            Hold hold = context.getBean(HoldClient.class).createHold(requested());

            assertThat(hold.getHoldId()).isEqualTo("hold-6c0c2f41");
            assertThat(hold.getCatedraProcessId()).isEqualTo("process-a92b47d0");
            assertThat(hold.getExpiresAt()).isEqualTo(HOLD_EXPIRES_AT);
        });
        catedra.verify(postRequestedFor(urlEqualTo(HOLDS))
                .withHeader("Authorization", equalTo("Bearer " + JWT))
                .withRequestBody(equalToJson("""
                        {"professionalId": 201, "date": "2026-11-02", "startTime": "12:30", "externalPatientId": "user-42"}""")));
    }

    @Test
    void aTakenSlotKeepsTheCodeOfTheCatedra() {
        catedra.stubFor(post(HOLDS).willReturn(problem(409, "SLOT_ALREADY_HELD")));

        contextRunner.run(context -> assertThatThrownBy(() -> context.getBean(HoldClient.class).createHold(requested()))
                .isInstanceOf(ReservationException.class)
                .extracting("code").isEqualTo("SLOT_ALREADY_HELD"));
    }

    @Test
    void creatingTheHoldIsNotRetried() {
        catedra.stubFor(post(HOLDS).willReturn(serviceUnavailable()));

        contextRunner.run(context -> assertThatThrownBy(() -> context.getBean(HoldClient.class).createHold(requested()))
                .isInstanceOf(ReservationException.class)
                .extracting("code").isEqualTo(ReservationException.CATEDRA_UNAVAILABLE));
        catedra.verify(1, postRequestedFor(urlEqualTo(HOLDS)));
    }

    @Test
    void anErrorWithoutCodeIsCatedraRejected() {
        catedra.stubFor(post(HOLDS).willReturn(aResponse().withStatus(403)));

        contextRunner.run(context -> assertThatThrownBy(() -> context.getBean(HoldClient.class).createHold(requested()))
                .isInstanceOf(ReservationException.class)
                .extracting("code").isEqualTo(ReservationException.CATEDRA_REJECTED));
    }

    @Test
    void confirmsTheHoldWithThePatientNames() {
        catedra.stubFor(post(CONFIRM).willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                .withBody("{\"reservationProcessId\": \"process-a92b47d0\", \"status\": \"WAITING_FOR_PHONE\","
                        + " \"expiresAt\": \"2026-10-29T15:20:00Z\"}")));

        contextRunner.run(context -> assertThat(context.getBean(HoldClient.class).confirmHold(held(), juan()))
                .isEqualTo(Instant.parse("2026-10-29T15:20:00Z")));
        catedra.verify(postRequestedFor(urlEqualTo(CONFIRM))
                .withHeader("Authorization", equalTo("Bearer " + JWT))
                .withRequestBody(equalToJson("""
                        {"externalPatientId": "user-42", "patientFirstName": "Juan", "patientLastName": "Perez"}""")));
    }

    @Test
    void confirmingIsRetriedAfterA5xx() {
        catedra.stubFor(post(CONFIRM).inScenario("caída").whenScenarioStateIs(STARTED)
                .willReturn(serviceUnavailable()).willSetStateTo("recuperada"));
        catedra.stubFor(post(CONFIRM).inScenario("caída").whenScenarioStateIs("recuperada")
                .willReturn(aResponse().withStatus(202).withHeader("Content-Type", "application/json")
                        .withBody("{\"status\": \"WAITING_FOR_PHONE\", \"expiresAt\": \"2026-10-29T15:20:00Z\"}")));

        contextRunner.run(context -> assertThat(context.getBean(HoldClient.class).confirmHold(held(), juan())).isNotNull());
        catedra.verify(2, postRequestedFor(urlEqualTo(CONFIRM)));
    }

    @Test
    void anAlreadyConfirmedHoldCountsAsConfirmed() {
        // Por ejemplo, la primera confirmación llegó pero su respuesta se perdió: el reintento responde 409.
        catedra.stubFor(post(CONFIRM).willReturn(problem(409, "RESERVATION_PROCESS_ALREADY_CONFIRMED")));

        contextRunner.run(context -> assertThat(context.getBean(HoldClient.class).confirmHold(held(), juan())).isNull());
    }

    @Test
    void anExpiredHoldIsReported() {
        catedra.stubFor(post(CONFIRM).willReturn(problem(404, "HOLD_EXPIRED")));

        contextRunner.run(context -> assertThatThrownBy(() -> context.getBean(HoldClient.class).confirmHold(held(), juan()))
                .isInstanceOf(ReservationException.class)
                .extracting("code").isEqualTo(ReservationException.HOLD_EXPIRED));
    }
}
