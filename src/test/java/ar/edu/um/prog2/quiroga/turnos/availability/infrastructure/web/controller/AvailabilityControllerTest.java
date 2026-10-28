package ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.web.controller;

import static ar.edu.um.prog2.quiroga.turnos.availability.support.AvailabilityFixtures.ANA_TORRES_ID;
import static ar.edu.um.prog2.quiroga.turnos.availability.support.AvailabilityFixtures.MONDAY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalTime;
import java.util.List;

import ar.edu.um.prog2.quiroga.turnos.availability.application.exception.AvailabilityException;
import ar.edu.um.prog2.quiroga.turnos.availability.application.service.AvailabilityService;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.Availability;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.AvailableSlot;
import ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.web.handler.AvailabilityExceptionHandler;
import ar.edu.um.prog2.quiroga.turnos.availability.infrastructure.web.mapper.AvailabilityDtoMapper;
import ar.edu.um.prog2.quiroga.turnos.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AvailabilityController.class)
// Sin los filtros de seguridad: acá se prueba el controller; el flujo con JWT está en AvailabilityFlowTest.
@AutoConfigureMockMvc(addFilters = false)
@Import({AvailabilityDtoMapper.class, AvailabilityExceptionHandler.class, GlobalExceptionHandler.class})
class AvailabilityControllerTest {

    private static final String URL = "/api/availability";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AvailabilityService availabilityService;

    @Test
    void returnsTheFreeSlotsOfTheDay() throws Exception {
        when(availabilityService.getAvailability(ANA_TORRES_ID, MONDAY)).thenReturn(Availability.builder()
                .professionalId(ANA_TORRES_ID)
                .date(MONDAY)
                .slots(List.of(
                        AvailableSlot.builder().date(MONDAY).startTime(LocalTime.of(12, 0)).endTime(LocalTime.of(12, 30)).build(),
                        AvailableSlot.builder().date(MONDAY).startTime(LocalTime.of(13, 0)).endTime(LocalTime.of(13, 30)).build()))
                .build());

        mockMvc.perform(get(URL).param("professionalId", "201").param("date", "2026-11-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.professionalId").value(201))
                .andExpect(jsonPath("$.date").value("2026-11-02"))
                .andExpect(jsonPath("$.slots.length()").value(2))
                .andExpect(jsonPath("$.slots[0].startTime").value("12:00:00"))
                .andExpect(jsonPath("$.slots[0].endTime").value("12:30:00"))
                .andExpect(jsonPath("$.slots[1].startTime").value("13:00:00"));
    }

    @ParameterizedTest
    @CsvSource({
            "INVALID_DATE, 400",
            "PROFESSIONAL_NOT_FOUND, 404",
            "PROFESSIONAL_DISABLED, 400",
            "CATALOG_UNAUTHORIZED, 401",
            "CATALOG_UNAVAILABLE, 503",
            "CATEDRA_UNAVAILABLE, 503"})
    void errorsOfTheSliceKeepTheirCodeAndStatus(String code, int status) throws Exception {
        when(availabilityService.getAvailability(anyLong(), any())).thenThrow(new AvailabilityException(code, "detalle"));

        mockMvc.perform(get(URL).param("professionalId", "201").param("date", "2026-11-02"))
                .andExpect(status().is(status))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(code));
    }

    @ParameterizedTest
    @CsvSource({
            "201, 02/11/2026",
            "0, 2026-11-02",
            "abc, 2026-11-02"})
    void invalidParametersAre400ValidationError(String professionalId, String date) throws Exception {
        mockMvc.perform(get(URL).param("professionalId", professionalId).param("date", date))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        verifyNoInteractions(availabilityService);
    }

    @Test
    void missingDateIs400ValidationError() throws Exception {
        mockMvc.perform(get(URL).param("professionalId", "201"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
