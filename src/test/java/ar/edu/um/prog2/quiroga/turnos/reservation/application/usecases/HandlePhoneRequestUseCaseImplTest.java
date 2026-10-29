package ar.edu.um.prog2.quiroga.turnos.reservation.application.usecases;

import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.NOW;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.held;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.phoneRequest;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.waitingForPhone;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;

import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationStatus;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.ProcessedEventRepository;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HandlePhoneRequestUseCaseImplTest {

    @Mock
    private ReservationProcessRepository reservationProcessRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    private HandlePhoneRequestUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new HandlePhoneRequestUseCaseImpl(reservationProcessRepository, processedEventRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private void processIs(ReservationProcess process) {
        when(processedEventRepository.existsByEventId("request-event-8a31")).thenReturn(false);
        when(reservationProcessRepository.findReservationProcessByCatedraProcessId("process-a92b47d0")).thenReturn(Optional.of(process));
    }

    @Test
    void theRequestIsSavedInTheProcessAndTheEventIsRecorded() {
        ReservationProcess process = waitingForPhone();
        processIs(process);

        useCase.handlePhoneRequest(phoneRequest());

        assertThat(process.isPhoneRequested()).isTrue();
        verify(reservationProcessRepository).saveReservationProcess(process);
        verify(processedEventRepository).saveProcessedEvent("request-event-8a31", "AdditionalInformationRequested");
    }

    @Test
    void aRepeatedEventDoesNothing() {
        when(processedEventRepository.existsByEventId("request-event-8a31")).thenReturn(true);

        useCase.handlePhoneRequest(phoneRequest());

        verify(reservationProcessRepository, never()).findReservationProcessByCatedraProcessId(anyString());
        verify(processedEventRepository, never()).saveProcessedEvent(anyString(), anyString());
    }

    @Test
    void anEventOfAnUnknownProcessIsOnlyRecorded() {
        when(processedEventRepository.existsByEventId("request-event-8a31")).thenReturn(false);
        when(reservationProcessRepository.findReservationProcessByCatedraProcessId("process-a92b47d0")).thenReturn(Optional.empty());

        useCase.handlePhoneRequest(phoneRequest());

        verify(reservationProcessRepository, never()).saveReservationProcess(any());
        verify(processedEventRepository).saveProcessedEvent("request-event-8a31", "AdditionalInformationRequested");
    }

    @Test
    void aLateEventDoesNotReopenAFinishedProcess() {
        ReservationProcess expired = waitingForPhone();
        expired.expire(NOW);
        processIs(expired);

        useCase.handlePhoneRequest(phoneRequest());

        assertThat(expired.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        verify(reservationProcessRepository, never()).saveReservationProcess(any());
        verify(processedEventRepository).saveProcessedEvent("request-event-8a31", "AdditionalInformationRequested");
    }

    @Test
    void anEventBeforeTheConfirmationIsSavedIsRetriedLater() {
        processIs(held());

        assertThatThrownBy(() -> useCase.handlePhoneRequest(phoneRequest()))
                .isInstanceOf(ReservationException.class)
                .extracting("code").isEqualTo(ReservationException.CONFIRMATION_IN_FLIGHT);
        // No se registra: cuando se reintente, se tiene que procesar.
        verify(processedEventRepository, never()).saveProcessedEvent(anyString(), anyString());
    }
}
