package ar.edu.um.prog2.quiroga.turnos.auth.infrastructure.web.controller;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.um.prog2.quiroga.turnos.auth.application.exception.AuthException;
import ar.edu.um.prog2.quiroga.turnos.auth.application.service.AuthService;
import ar.edu.um.prog2.quiroga.turnos.auth.domain.model.AccessToken;
import ar.edu.um.prog2.quiroga.turnos.auth.infrastructure.web.handler.AuthExceptionHandler;
import ar.edu.um.prog2.quiroga.turnos.auth.infrastructure.web.mapper.AuthDtoMapper;
import ar.edu.um.prog2.quiroga.turnos.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
// Sin los filtros de seguridad: acá se prueba el controller; el flujo con seguridad está en AuthenticationFlowTest.
@AutoConfigureMockMvc(addFilters = false)
@Import({AuthDtoMapper.class, AuthExceptionHandler.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void validLoginReturnsTheTokenInTheBodyAndInTheHeader() throws Exception {
        when(authService.authenticate("juan.perez", "una-clave-segura", true))
                .thenReturn(AccessToken.builder().value("el-token").build());

        mockMvc.perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"juan.perez\", \"password\": \"una-clave-segura\", \"rememberMe\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id_token").value("el-token"))
                .andExpect(header().string("Authorization", "Bearer el-token"));
    }

    @Test
    void wrongCredentialsReturn401WithItsCode() throws Exception {
        when(authService.authenticate(anyString(), anyString(), anyBoolean()))
                .thenThrow(new AuthException("Usuario o contraseña incorrectos", new IllegalStateException()));

        mockMvc.perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"juan.perez\", \"password\": \"mala-clave\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));
    }

    @Test
    void missingFieldsReturn400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/authenticate").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
    }
}
