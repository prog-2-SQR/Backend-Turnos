package ar.edu.um.prog2.quiroga.turnos.reservation.application.usecases;

import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.NOW;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.juan;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.phoneSubmitted;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.result;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;

import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.Patient;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationResultType;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationStatus;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.CatedraReservationClient;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.PatientDirectory;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CancelReservationUseCaseImplTest {

    @Mock
    private PatientDirectory patientDirectory;

    @Mock
    private ReservationProcessRepository reservationProcessRepository;

    @Mock
    private CatedraReservationClient catedraReservationClient;

    private CancelReservationUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new CancelReservationUseCaseImpl(patientDirectory, reservationProcessRepository, catedraReservationClient,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static ReservationProcess confirmed() {
        ReservationProcess process = phoneSubmitted();
        process.applyResult(result(ReservationResultType.CONFIRMED), NOW);
        return process;
    }

    private void juansProcessIs(ReservationProcess process) {
        when(patientDirectory.findPatient("juan.perez")).thenReturn(juan());
        when(reservationProcessRepository.findReservationProcessById(process.getId())).thenReturn(Optional.of(process));
    }

    @Test
    void aConfirmedReservationIsCancelledInTheCatedraAndThenHere() {
        ReservationProcess process = confirmed();
        juansProcessIs(process);
        when(reservationProcessRepository.saveReservationProcess(process)).thenReturn(process);

        ReservationProcess result = useCase.cancelReservation("juan.perez", process.getId(), "No puedo ir");

        verify(catedraReservationClient).cancelReservation("process-a92b47d0", "No puedo ir");
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        verify(reservationProcessRepository).saveReservationProcess(process);
    }

    @Test
    void ifTheCatedraFailsNothingChangesHere() {
        ReservationProcess process = confirmed();
        juansProcessIs(process);
        doThrow(new ReservationException(ReservationException.CATEDRA_UNAVAILABLE, "caída"))
                .when(catedraReservationClient).cancelReservation(anyString(), any());

        assertThatThrownBy(() -> useCase.cancelReservation("juan.perez", process.getId(), null))
                .isInstanceOf(ReservationException.class)
                .extracting("code").isEqualTo(ReservationException.CATEDRA_UNAVAILABLE);
        assertThat(process.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        verify(reservationProcessRepository, never()).saveReservationProcess(any());
    }

    @Test
    void anAlreadyCancelledReservationIsReturnedAsItIs() {
        ReservationProcess process = confirmed();
        process.cancel(NOW);
        juansProcessIs(process);

        assertThat(useCase.cancelReservation("juan.perez", process.getId(), null)).isSameAs(process);
        verifyNoInteractions(catedraReservationClient);
    }

    @Test
    void aReservationInProgressCannotBeCancelled() {
        ReservationProcess process = phoneSubmitted();
        juansProcessIs(process);

        assertThatThrownBy(() -> useCase.cancelReservation("juan.perez", process.getId(), null))
                .isInstanceOf(ReservationException.class)
                .extracting("code").isEqualTo(ReservationException.RESERVATION_NOT_CANCELLABLE);
        verifyNoInteractions(catedraReservationClient);
    }

    @Test
    void theReservationOfAnotherUserIsNotFound() {
        ReservationProcess process = confirmed();
        when(reservationProcessRepository.findReservationProcessById(process.getId())).thenReturn(Optional.of(process));
        when(patientDirectory.findPatient("otro")).thenReturn(Patient.builder().userId(99L).build());

        assertThatThrownBy(() -> useCase.cancelReservation("otro", process.getId(), null))
                .isInstanceOf(ReservationException.class)
                .extracting("code").isEqualTo(ReservationException.RESERVATION_NOT_FOUND);
        verifyNoInteractions(catedraReservationClient);
    }
}
