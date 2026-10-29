package ar.edu.um.prog2.quiroga.turnos.reservation;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import ar.edu.um.prog2.quiroga.turnos.TestSecrets;
import ar.edu.um.prog2.quiroga.turnos.TestcontainersConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Registro → login → reserva, con la seguridad real y PostgreSQL. Un WireMock hace de catálogo (profesional y
 * horarios) y otro de cátedra (ocupaciones, hold y confirmación).
 */
@SpringBootTest(properties = {"security.jwt.base64-secret=" + TestSecrets.JWT_SECRET, "catedra.jwt=jwt-tecnico-de-prueba",
        "catalogo.retry-delay=10ms", "catedra.retry-delay=10ms"})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ReservationFlowTest {

    // Un lunes futuro, para que el test no dependa del día en que corre.
    private static final LocalDate NEXT_MONDAY = LocalDate.now().plusDays(1).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    private static final String RESERVATION = """
            {"professionalId": 201, "date": "%s", "startTime": "12:30"}""".formatted(NEXT_MONDAY);
    private static final String EXPIRES_AT = Instant.now().plusSeconds(600).toString();

    @RegisterExtension
    static WireMockExtension catalogo = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    @RegisterExtension
    static WireMockExtension catedra = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    @DynamicPropertySource
    static void remoteServices(DynamicPropertyRegistry registry) {
        registry.add("catalogo.api-url", catalogo::baseUrl);
        registry.add("catedra.api-url", catedra::baseUrl);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void anaTorresAttendsOnMondaysAndTheCatedraAcceptsTheHold() {
        catalogo.stubFor(get("/api/professionals/201").willReturn(okJson("""
                {"id": 201, "categoryId": 101, "firstName": "Ana", "lastName": "Torres", "enabled": true}""")));
        catalogo.stubFor(get(urlEqualTo("/api/schedules?professionalId=201")).willReturn(okJson("""
                [{"id": 301, "professionalId": 201, "dayOfWeek": "MONDAY", "startTime": "12:00:00",
                  "endTime": "13:30:00", "slotDurationMinutes": 30, "enabled": true}]""")));
        catedra.stubFor(get(urlPathEqualTo("/api/appointment-occupancies")).willReturn(okJson("""
                {"professionalId": 201, "occupancies": []}""")));
        holdAccepted();
        catedra.stubFor(post(urlPathEqualTo("/api/appointment-holds/hold-1/confirm")).willReturn(aResponse().withStatus(202)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"reservationProcessId\": \"process-1\", \"status\": \"WAITING_FOR_PHONE\", \"expiresAt\": \"" + EXPIRES_AT + "\"}")));
    }

    private static void holdAccepted() {
        catedra.stubFor(post("/api/appointment-holds").willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                .withBody("{\"holdId\": \"hold-1\", \"reservationProcessId\": \"process-1\", \"status\": \"HELD\", \"expiresAt\": \"" + EXPIRES_AT + "\"}")));
    }

    @AfterEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE reservation_process, jhi_user CASCADE");
    }

    private String registerAndLogIn(String login) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"login": "%s", "password": "una-clave-segura", "email": "%s@example.com",
                         "firstName": "Juan", "lastName": "Perez"}""".formatted(login, login)))
                .andExpect(status().isCreated());
        String body = mockMvc.perform(MockMvcRequestBuilders.post("/api/authenticate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"%s\", \"password\": \"una-clave-segura\"}".formatted(login)))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.id_token");
    }

    private String startReservation(String token, int expectedStatus) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post("/api/reservations").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(RESERVATION))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void theReservationIsHeldConfirmedAndWaitsForThePhone() throws Exception {
        String token = registerAndLogIn("juan.perez");
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM jhi_user WHERE login = 'juan.perez'", Long.class);

        String body = startReservation(token, 201);

        assertThat(JsonPath.<String>read(body, "$.status")).isEqualTo("WAITING_FOR_PHONE");
        String id = JsonPath.read(body, "$.id");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM reservation_process WHERE id = ?::uuid", String.class, id))
                .isEqualTo("WAITING_FOR_PHONE");
        // A la cátedra le llega el paciente como user-<id>, con el JWT técnico; los nombres salen de la cuenta.
        catedra.verify(postRequestedFor(urlEqualTo("/api/appointment-holds"))
                .withHeader("Authorization", equalTo("Bearer jwt-tecnico-de-prueba"))
                .withRequestBody(matchingJsonPath("$.externalPatientId", equalTo("user-" + userId))));
        catedra.verify(postRequestedFor(urlEqualTo("/api/appointment-holds/hold-1/confirm"))
                .withRequestBody(matchingJsonPath("$.patientFirstName", equalTo("Juan"))));
        mockMvc.perform(MockMvcRequestBuilders.get("/api/reservations/{id}", id).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_FOR_PHONE"));
    }

    @Test
    void aSecondReservationWhileTheFirstIsInProgressIs409() throws Exception {
        String token = registerAndLogIn("juan.perez");
        startReservation(token, 201);

        String body = startReservation(token, 409);

        assertThat(JsonPath.<String>read(body, "$.code")).isEqualTo("RESERVATION_ALREADY_PENDING");
    }

    @Test
    void anotherUserDoesNotSeeTheReservation() throws Exception {
        String id = JsonPath.read(startReservation(registerAndLogIn("juan.perez"), 201), "$.id");

        mockMvc.perform(MockMvcRequestBuilders.get("/api/reservations/{id}", id).header("Authorization", registerAndLogIn("ana.gomez")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND"));
    }

    @Test
    void aSlotTakenAtTheCatedraFailsAndAllowsTryingAgain() throws Exception {
        String token = registerAndLogIn("juan.perez");
        catedra.stubFor(post("/api/appointment-holds").willReturn(aResponse().withStatus(409)
                .withHeader("Content-Type", "application/problem+json")
                .withBody("{\"status\": 409, \"code\": \"SLOT_ALREADY_HELD\", \"detail\": \"Slot already held\"}")));

        String body = startReservation(token, 409);

        assertThat(JsonPath.<String>read(body, "$.code")).isEqualTo("SLOT_ALREADY_HELD");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM reservation_process", String.class)).isEqualTo("FAILED");
        // Un proceso terminado no bloquea: el usuario puede volver a intentar.
        holdAccepted();
        startReservation(token, 201);
    }

    @Test
    void withoutTokenIs401() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(RESERVATION))
                .andExpect(status().isUnauthorized());
    }
}
