package ar.edu.um.prog2.quiroga.turnos.availability;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.DayOfWeek;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Registro → login → disponibilidad, con la seguridad real y PostgreSQL. Un WireMock hace de catálogo y otro de
 * cátedra, para comprobar qué token recibe cada uno.
 */
@SpringBootTest(properties = {"security.jwt.base64-secret=" + TestSecrets.JWT_SECRET, "catedra.jwt=jwt-tecnico-de-prueba",
        "catalogo.retry-delay=10ms", "catedra.retry-delay=10ms"})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AvailabilityFlowTest {

    // Un lunes futuro, para que el test no dependa del día en que corre.
    private static final LocalDate NEXT_MONDAY = LocalDate.now().plusDays(1).with(TemporalAdjusters.next(DayOfWeek.MONDAY));

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

    private String token;

    @BeforeEach
    void logIn() throws Exception {
        mockMvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"login": "juan.perez", "password": "una-clave-segura", "email": "juan.perez@example.com"}"""))
                .andExpect(status().isCreated());
        String body = mockMvc.perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"juan.perez\", \"password\": \"una-clave-segura\"}"))
                .andReturn().getResponse().getContentAsString();
        token = JsonPath.read(body, "$.id_token");
    }

    @AfterEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE jhi_user CASCADE");
    }

    @Test
    void freeSlotsComeFromTheCatalogAndTheCatedraEachWithItsOwnToken() throws Exception {
        catalogo.stubFor(get("/api/professionals/201").willReturn(okJson("""
                {"id": 201, "categoryId": 101, "firstName": "Ana", "lastName": "Torres", "enabled": true}""")));
        catalogo.stubFor(get(urlEqualTo("/api/schedules?professionalId=201")).willReturn(okJson("""
                [{"id": 301, "professionalId": 201, "dayOfWeek": "MONDAY", "startTime": "12:00:00",
                  "endTime": "13:30:00", "slotDurationMinutes": 30, "enabled": true}]""")));
        catedra.stubFor(get(urlPathEqualTo("/api/appointment-occupancies")).willReturn(okJson("""
                {"professionalId": 201, "from": "%1$s", "to": "%1$s",
                 "occupancies": [{"date": "%1$s", "startTime": "12:30:00", "status": "HELD", "expiresAt": "2030-01-01T00:00:00Z"}]}
                """.formatted(NEXT_MONDAY))));

        mockMvc.perform(availability().header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots.length()").value(2))
                .andExpect(jsonPath("$.slots[0].startTime").value("12:00:00"))
                .andExpect(jsonPath("$.slots[1].startTime").value("13:00:00"));

        // Token relay: el catálogo recibe el token del usuario; la cátedra, el JWT técnico.
        catalogo.verify(getRequestedFor(urlEqualTo("/api/professionals/201")).withHeader("Authorization", equalTo("Bearer " + token)));
        catedra.verify(getRequestedFor(urlPathEqualTo("/api/appointment-occupancies"))
                .withHeader("Authorization", equalTo("Bearer jwt-tecnico-de-prueba")));
    }

    @Test
    void catalogDownIs503CatalogUnavailable() throws Exception {
        catalogo.stubFor(get("/api/professionals/201").willReturn(serviceUnavailable()));

        mockMvc.perform(availability().header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("CATALOG_UNAVAILABLE"));
    }

    @Test
    void withoutTokenIs401() throws Exception {
        mockMvc.perform(availability()).andExpect(status().isUnauthorized());
    }

    private static MockHttpServletRequestBuilder availability() {
        return MockMvcRequestBuilders.get("/api/availability")
                .param("professionalId", "201")
                .param("date", NEXT_MONDAY.toString());
    }
}
