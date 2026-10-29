package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.web.controller;

import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.HOLD_EXPIRES_AT;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.NOW;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.phoneRequest;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.waitingForPhone;
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

import java.time.Instant;
import java.util.UUID;

import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import ar.edu.um.prog2.quiroga.turnos.reservation.application.service.ReservationService;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationRequest;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.web.handler.ReservationExceptionHandler;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.web.mapper.ReservationDtoMapper;
import ar.edu.um.prog2.quiroga.turnos.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

@WebMvcTest(ReservationController.class)
// Sin los filtros de seguridad: acá se prueba el controller; el flujo con JWT está en ReservationFlowTest.
@AutoConfigureMockMvc(addFilters = false)
@Import({ReservationDtoMapper.class, ReservationExceptionHandler.class, GlobalExceptionHandler.class})
class ReservationControllerTest {

    private static final String BODY = """
            {"professionalId": 201, "date": "2026-11-02", "startTime": "12:30"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservationService reservationService;

    // Sin los filtros de seguridad, el usuario del token se carga a mano (es lo que lee @AuthenticationPrincipal).
    @BeforeEach
    void loggedInAsJuan() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS512").subject("juan.perez").issuedAt(Instant.now()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void startsTheReservationOfTheUserOfTheToken() throws Exception {
        ReservationProcess process = waitingForPhone();
        when(reservationService.startReservation(eq("juan.perez"), any(ReservationRequest.class))).thenReturn(process);

        mockMvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(process.getId().toString()))
                .andExpect(jsonPath("$.status").value("WAITING_FOR_PHONE"))
                .andExpect(jsonPath("$.professionalId").value(201))
                .andExpect(jsonPath("$.date").value("2026-11-02"))
                .andExpect(jsonPath("$.startTime").value("12:30:00"))
                .andExpect(jsonPath("$.expiresAt").value(HOLD_EXPIRES_AT.toString()))
                .andExpect(jsonPath("$.holdId").doesNotExist());
    }

    @Test
    void anInvalidBodyIs400WithTheFields() throws Exception {
        mockMvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"professionalId\": 0, \"patientFirstName\": \"J\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'professionalId')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'date')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'startTime')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'patientFirstName')]").exists());
        verify(reservationService, never()).startReservation(any(), any());
    }

    @ParameterizedTest
    @CsvSource({
            "RESERVATION_ALREADY_PENDING, 409",
            "PATIENT_NAME_REQUIRED, 400",
            "SLOT_NOT_AVAILABLE, 409",
            "SLOT_ALREADY_HELD, 409",
            "SLOT_ALREADY_RESERVED, 409",
            "INVALID_SLOT, 400",
            "HOLD_EXPIRED, 409",
            "PROFESSIONAL_NOT_FOUND, 404",
            "PROFESSIONAL_DISABLED, 400",
            "CATALOG_UNAVAILABLE, 503",
            "CATEDRA_UNAVAILABLE, 503",
            "CATEDRA_REJECTED, 502",
            "UN_CODE_NUEVO_DE_LA_CATEDRA, 502"})
    void errorsKeepTheirCode(String code, int status) throws Exception {
        when(reservationService.startReservation(any(), any())).thenThrow(new ReservationException(code, "detalle"));

        mockMvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().is(status))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(code));
    }

    @Test
    void returnsAProcessOfTheUser() throws Exception {
        ReservationProcess process = waitingForPhone();
        when(reservationService.getReservation("juan.perez", process.getId())).thenReturn(process);

        mockMvc.perform(get("/api/reservations/{id}", process.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_FOR_PHONE"));
    }

    @Test
    void showsWhenTheCatedraAskedForThePhone() throws Exception {
        ReservationProcess process = waitingForPhone();
        process.receivePhoneRequest(phoneRequest(), NOW);
        when(reservationService.getReservation("juan.perez", process.getId())).thenReturn(process);

        mockMvc.perform(get("/api/reservations/{id}", process.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phoneRequested").value(true))
                .andExpect(jsonPath("$.phoneRequestMessage").value("Ingrese un numero de telefono para continuar."));
    }

    @Test
    void anUnknownProcessIs404() throws Exception {
        UUID id = UUID.randomUUID();
        when(reservationService.getReservation("juan.perez", id))
                .thenThrow(new ReservationException(ReservationException.RESERVATION_NOT_FOUND, "No existe la reserva " + id));

        mockMvc.perform(get("/api/reservations/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND"));
    }
}
