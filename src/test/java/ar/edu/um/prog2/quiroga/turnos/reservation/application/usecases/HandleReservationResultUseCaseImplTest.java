package ar.edu.um.prog2.quiroga.turnos.reservation.application.usecases;

import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.NOW;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.held;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.phoneSubmitted;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.result;
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
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationResult;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationResultType;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationStatus;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.ProcessedEventRepository;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HandleReservationResultUseCaseImplTest {

    @Mock
    private ReservationProcessRepository reservationProcessRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    private HandleReservationResultUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new HandleReservationResultUseCaseImpl(reservationProcessRepository, processedEventRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private void processIs(ReservationProcess process) {
        when(processedEventRepository.existsByEventId("result-event-7d10")).thenReturn(false);
        when(reservationProcessRepository.findReservationProcessByCatedraProcessId("process-a92b47d0")).thenReturn(Optional.of(process));
    }

    @Test
    void aConfirmationIsSavedAndTheEventIsRecorded() {
        ReservationProcess process = phoneSubmitted();
        processIs(process);
        ReservationResult confirmed = result(ReservationResultType.CONFIRMED);
        confirmed.setCatedraReservationId(8251L);

        useCase.handleReservationResult(confirmed);

        assertThat(process.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(process.getCatedraReservationId()).isEqualTo(8251L);
        verify(reservationProcessRepository).saveReservationProcess(process);
        verify(processedEventRepository).saveProcessedEvent("result-event-7d10", "CONFIRMED");
    }

    @Test
    void aRepeatedEventDoesNothing() {
        when(processedEventRepository.existsByEventId("result-event-7d10")).thenReturn(true);

        useCase.handleReservationResult(result(ReservationResultType.CONFIRMED));

        verify(reservationProcessRepository, never()).findReservationProcessByCatedraProcessId(anyString());
        verify(processedEventRepository, never()).saveProcessedEvent(anyString(), anyString());
    }

    @Test
    void anEventOfAnUnknownProcessIsOnlyRecorded() {
        when(processedEventRepository.existsByEventId("result-event-7d10")).thenReturn(false);
        when(reservationProcessRepository.findReservationProcessByCatedraProcessId("process-a92b47d0")).thenReturn(Optional.empty());

        useCase.handleReservationResult(result(ReservationResultType.EXPIRED));

        verify(reservationProcessRepository, never()).saveReservationProcess(any());
        verify(processedEventRepository).saveProcessedEvent("result-event-7d10", "EXPIRED");
    }

    @Test
    void aLateEventDoesNotReopenAFinalState() {
        ReservationProcess process = phoneSubmitted();
        process.applyResult(result(ReservationResultType.CONFIRMED), NOW);
        processIs(process);

        useCase.handleReservationResult(result(ReservationResultType.EXPIRED));

        assertThat(process.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        verify(reservationProcessRepository, never()).saveReservationProcess(any());
        verify(processedEventRepository).saveProcessedEvent("result-event-7d10", "EXPIRED");
    }

    @Test
    void aResultBeforeTheConfirmationIsSavedIsRetried() {
        processIs(held());

        assertThatThrownBy(() -> useCase.handleReservationResult(result(ReservationResultType.CONFIRMED)))
                .isInstanceOf(ReservationException.class)
                .extracting("code").isEqualTo(ReservationException.CONFIRMATION_IN_FLIGHT);
        verify(processedEventRepository, never()).saveProcessedEvent(anyString(), anyString());
    }

    @Test
    void anExpirationWhileHeldIsApplied() {
        ReservationProcess process = held();
        processIs(process);

        useCase.handleReservationResult(result(ReservationResultType.EXPIRED));

        assertThat(process.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        verify(reservationProcessRepository).saveReservationProcess(process);
    }
}
