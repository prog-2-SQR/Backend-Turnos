package ar.edu.um.prog2.quiroga.turnos.user.infrastructure.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
}
