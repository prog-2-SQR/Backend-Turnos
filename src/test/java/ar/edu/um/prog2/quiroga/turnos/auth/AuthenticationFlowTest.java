package ar.edu.um.prog2.quiroga.turnos.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;
import java.util.Set;

import ar.edu.um.prog2.quiroga.turnos.TestSecrets;
import ar.edu.um.prog2.quiroga.turnos.TestcontainersConfiguration;
import ar.edu.um.prog2.quiroga.turnos.auth.domain.model.AuthenticatedUser;
import ar.edu.um.prog2.quiroga.turnos.auth.infrastructure.security.JwtTokenIssuer;
import ar.edu.um.prog2.quiroga.turnos.shared.security.JwtConfig;
import ar.edu.um.prog2.quiroga.turnos.shared.security.JwtProperties;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

/** Registro → login → request protegido, con la cadena de seguridad real y PostgreSQL (Testcontainers). */
@SpringBootTest(properties = "security.jwt.base64-secret=" + TestSecrets.JWT_SECRET)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthenticationFlowTest {

    private static final String REGISTER = """
            {"login": "Juan.Perez", "password": "una-clave-segura", "email": "juan.perez@example.com"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE jhi_user CASCADE");
    }

    @Test
    void aRegisteredUserCanLogInAndUseTheToken() throws Exception {
        mockMvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER))
                .andExpect(status().isCreated());

        String body = mockMvc.perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"JUAN.PEREZ\", \"password\": \"una-clave-segura\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.id_token");

        Jwt jwt = jwtDecoder.decode(token);
        assertThat(jwt.getSubject()).isEqualTo("juan.perez");
        assertThat(jwt.getClaimAsString("auth")).isEqualTo("ROLE_USER");
        // Con el token se accede a la cuenta propia; sin token, 401.
        mockMvc.perform(get("/api/account").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("juan.perez"))
                .andExpect(jsonPath("$.email").value("juan.perez@example.com"));
        mockMvc.perform(get("/api/account"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordAndUnknownUserGetTheSame401() throws Exception {
        mockMvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"juan.perez\", \"password\": \"otra-clave\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));
        mockMvc.perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"nadie\", \"password\": \"otra-clave\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));
    }

    @Test
    void aMalformedTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/protegido").header("Authorization", "Bearer no.es.un-jwt"))
                .andExpect(status().isUnauthorized());
    }

    // Un token "admin" bien formado pero firmado con otro secreto: la firma no coincide.
    @Test
    void aTokenSignedWithAnotherSecretIsRejected() throws Exception {
        JwtProperties otherSecret = new JwtProperties();
        otherSecret.setBase64Secret(Base64.getEncoder().encodeToString("otro-secreto-de-64-bytes-que-no-es-el-del-servidor-xxxxxxxxxxxxx".getBytes()));
        JwtConfig jwtConfig = new JwtConfig();
        JwtTokenIssuer forger = new JwtTokenIssuer(jwtConfig.jwtEncoder(jwtConfig.jwtSecretKey(otherSecret)), otherSecret);
        String forged = forger.issue(AuthenticatedUser.builder().login("admin").authorities(Set.of("ROLE_ADMIN")).build(), false)
                .getValue();

        mockMvc.perform(get("/api/protegido").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
