package ar.edu.um.prog2.quiroga.turnos.reservation.application.usecases;

import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.NOW;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.juan;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.mondayAtTwelveThirty;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.waitingForPhone;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;

import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.CatedraReservation;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.CatedraReservationStatus;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.Hold;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationStatus;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.CatedraReservationClient;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReconcileReservationsUseCaseImplTest {

    @Mock
    private ReservationProcessRepository reservationProcessRepository;

    @Mock
    private CatedraReservationClient catedraReservationClient;

    private ReconcileReservationsUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ReconcileReservationsUseCaseImpl(reservationProcessRepository, catedraReservationClient,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    // Un proceso de Ana (usuario 7) esperando el teléfono, con su propio proceso en la cátedra.
    private static ReservationProcess anasProcess() {
        ReservationProcess process = ReservationProcess.request(juan().toBuilder().userId(7L).build(), mondayAtTwelveThirty(), NOW);
        process.hold(Hold.builder().holdId("hold-ana").catedraProcessId("process-ana").expiresAt(NOW.plusSeconds(600)).build(), NOW);
        process.waitForPhone(null, NOW);
        return process;
    }

    private static CatedraReservation inCatedra(String catedraProcessId, CatedraReservationStatus status) {
        return CatedraReservation.builder().catedraProcessId(catedraProcessId).catedraReservationId(1258L).status(status).build();
    }

    @Test
    void eachPatientIsAskedOnceAndOnlyTheChangedProcessesAreSaved() {
        ReservationProcess juans = waitingForPhone();
        ReservationProcess anas = anasProcess();
        when(reservationProcessRepository.findReconcilableReservationProcesses()).thenReturn(List.of(juans, anas));
        when(catedraReservationClient.findReservationsOfPatient("user-42"))
                .thenReturn(List.of(inCatedra("process-a92b47d0", CatedraReservationStatus.FAILED), inCatedra("otro", CatedraReservationStatus.CONFIRMED)));
        when(catedraReservationClient.findReservationsOfPatient("user-7"))
                .thenReturn(List.of(inCatedra("process-ana", CatedraReservationStatus.PHONE_PENDING)));

        assertThat(useCase.reconcilePendingReservations()).isEqualTo(1);

        assertThat(juans.getStatus()).isEqualTo(ReservationStatus.FAILED);
        assertThat(anas.getStatus()).isEqualTo(ReservationStatus.WAITING_FOR_PHONE);
        verify(reservationProcessRepository).saveReservationProcess(juans);
        verify(reservationProcessRepository, never()).saveReservationProcess(anas);
    }

    @Test
    void aProcessThatTheCatedraDoesNotListIsLeftAsItIs() {
        ReservationProcess juans = waitingForPhone();
        when(reservationProcessRepository.findReconcilableReservationProcesses()).thenReturn(List.of(juans));
        when(catedraReservationClient.findReservationsOfPatient("user-42")).thenReturn(List.of());

        assertThat(useCase.reconcilePendingReservations()).isZero();
        verify(reservationProcessRepository, never()).saveReservationProcess(any());
    }

    @Test
    void withNothingInProgressTheCatedraIsNotAsked() {
        when(reservationProcessRepository.findReconcilableReservationProcesses()).thenReturn(List.of());

        assertThat(useCase.reconcilePendingReservations()).isZero();
        verify(catedraReservationClient, never()).findReservationsOfPatient(any());
    }

    @Test
    void aCatedraThatDoesNotAnswerStopsTheRound() {
        when(reservationProcessRepository.findReconcilableReservationProcesses()).thenReturn(List.of(waitingForPhone()));
        when(catedraReservationClient.findReservationsOfPatient("user-42"))
                .thenThrow(new ReservationException(ReservationException.CATEDRA_UNAVAILABLE, "caída"));

        assertThatThrownBy(() -> useCase.reconcilePendingReservations()).isInstanceOf(ReservationException.class);
        verify(reservationProcessRepository, never()).saveReservationProcess(any());
    }
}
