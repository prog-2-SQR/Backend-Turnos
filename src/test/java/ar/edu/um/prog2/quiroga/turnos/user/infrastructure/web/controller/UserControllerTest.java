package ar.edu.um.prog2.quiroga.turnos.user.infrastructure.web.controller;

import static ar.edu.um.prog2.quiroga.turnos.user.support.UserFixtures.juan;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.um.prog2.quiroga.turnos.shared.web.GlobalExceptionHandler;
import ar.edu.um.prog2.quiroga.turnos.user.application.exception.UserException;
import ar.edu.um.prog2.quiroga.turnos.user.application.service.UserService;
import ar.edu.um.prog2.quiroga.turnos.user.domain.model.User;
import ar.edu.um.prog2.quiroga.turnos.user.infrastructure.web.handler.UserExceptionHandler;
import ar.edu.um.prog2.quiroga.turnos.user.infrastructure.web.mapper.UserDtoMapper;
import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
// Sin los filtros de seguridad: acá se prueba el controller; la seguridad tiene sus propios tests.
@AutoConfigureMockMvc(addFilters = false)
@Import({UserDtoMapper.class, UserExceptionHandler.class, GlobalExceptionHandler.class})
class UserControllerTest {

    private static final String VALID_BODY = """
            {"login": "juan.perez", "password": "una-clave-segura", "firstName": "Juan", "lastName": "Perez",
             "email": "juan.perez@example.com", "langKey": "es"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void registerReturns201WithoutBody() throws Exception {
        mockMvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));
        verify(userService).registerUser(any(User.class), eq("una-clave-segura"));
    }

    @Test
    void invalidFieldsReturn400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\": \"ju an\", \"password\": \"una-clave-segura\", \"email\": \"no-es-un-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'login')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'email')]").exists())
                .andExpect(jsonPath("$.fieldErrors[0].objectName").value("registerUserRequest"));
        verify(userService, never()).registerUser(any(), any());
    }

    @Test
    void loginInUseReturns400WithItsCode() throws Exception {
        when(userService.registerUser(any(User.class), any()))
                .thenThrow(new UserException(UserException.USERNAME_ALREADY_EXISTS, "El login ya está en uso"));

        mockMvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("USERNAME_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.detail").value("El login ya está en uso"));
    }

    @Test
    void invalidPasswordReturns400WithItsCode() throws Exception {
        when(userService.registerUser(any(User.class), any()))
                .thenThrow(new UserException(UserException.INVALID_PASSWORD, "La contraseña debe tener entre 4 y 100 caracteres"));

        mockMvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\": \"juan.perez\", \"password\": \"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // Sin los filtros de seguridad, el usuario del token se carga a mano (es lo que lee @AuthenticationPrincipal).
    private static void loggedInAs(String login) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS512").subject(login).issuedAt(Instant.now()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @Test
    void accountReturnsTheUserOfTheToken() throws Exception {
        when(userService.findUserByLogin("juan.perez")).thenReturn(juan());
        loggedInAs("juan.perez");

        mockMvc.perform(get("/api/account"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("juan.perez"))
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void accountOfADeletedUserReturns404WithItsCode() throws Exception {
        when(userService.findUserByLogin("borrado"))
                .thenThrow(new UserException(UserException.USER_NOT_FOUND, "No existe el usuario borrado"));
        loggedInAs("borrado");

        mockMvc.perform(get("/api/account"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }
}
