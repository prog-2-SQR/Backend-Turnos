package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.messaging.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import ar.edu.um.prog2.quiroga.turnos.reservation.application.service.ReservationService;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.PhoneRequest;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.messaging.mapper.ReservationActionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class ReservationActionsListenerTest {

    // Ejemplo del anexo §15.4, con un campo nuevo que tiene que ignorarse.
    private static final String PHONE_REQUESTED = """
            {"eventId": "request-event-8a31", "eventType": "AdditionalInformationRequested",
             "reservationProcessId": "process-a92b47d0", "holdId": "hold-6c0c2f41", "requiredField": "PHONE_NUMBER",
             "message": "Ingrese un numero de telefono para continuar.", "expiresAt": "2026-09-10T14:44:50Z",
             "occurredAt": "2026-09-10T14:35:10Z", "schemaVersion": 1, "campoNuevo": "x"}""";

    @Mock
    private ReservationService reservationService;

    private ReservationActionsListener listener;

    @BeforeEach
    void setUp() {
        listener = new ReservationActionsListener(reservationService, JsonMapper.builder().build(), new ReservationActionMapper());
    }

    @Test
    void aPhoneRequestReachesTheReservation() {
        listener.onAction(PHONE_REQUESTED);

        ArgumentCaptor<PhoneRequest> request = ArgumentCaptor.forClass(PhoneRequest.class);
        verify(reservationService).handlePhoneRequest(request.capture());
        assertThat(request.getValue().getEventId()).isEqualTo("request-event-8a31");
        assertThat(request.getValue().getCatedraProcessId()).isEqualTo("process-a92b47d0");
        assertThat(request.getValue().getMessage()).isEqualTo("Ingrese un numero de telefono para continuar.");
    }

    @Test
    void anEventThatIsNotHandledYetIsOnlyLogged() {
        listener.onAction("""
                {"eventId": "confirmation-event-7d10", "eventType": "AppointmentConfirmed", "reservationProcessId": "process-a92b47d0"}""");

        verifyNoInteractions(reservationService);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "no es json",
            "{\"eventType\": \"AdditionalInformationRequested\", \"reservationProcessId\": \"process-1\"}",
            "{\"eventId\": \"e-1\", \"reservationProcessId\": \"process-1\"}",
            "{\"eventId\": \"e-1\", \"eventType\": \"AdditionalInformationRequested\", \"reservationProcessId\": \" \"}",
            "null"})
    void unusableMessagesAreDiscarded(String payload) {
        listener.onAction(payload);

        verify(reservationService, never()).handlePhoneRequest(any());
    }

    @Test
    void aLongDiscardedMessageDoesNotBreakTheListener() {
        listener.onAction("x".repeat(500));

        verifyNoInteractions(reservationService);
    }

    @Test
    void errorsReachTheErrorHandlerToBeRetried() {
        doThrow(new ReservationException(ReservationException.CONFIRMATION_IN_FLIGHT, "todavía no"))
                .when(reservationService).handlePhoneRequest(any());

        assertThatThrownBy(() -> listener.onAction(PHONE_REQUESTED)).isInstanceOf(ReservationException.class);
    }
}
