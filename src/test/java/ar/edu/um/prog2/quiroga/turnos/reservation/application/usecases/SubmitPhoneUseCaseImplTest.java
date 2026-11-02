package ar.edu.um.prog2.quiroga.turnos.reservation.application.usecases;

import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.NOW;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.juan;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.phoneRequested;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.waitingForPhone;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.Patient;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.PhoneSubmission;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationStatus;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.PatientDirectory;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.PhoneSubmissionOutbox;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubmitPhoneUseCaseImplTest {

    @Mock
    private PatientDirectory patientDirectory;

    @Mock
    private ReservationProcessRepository reservationProcessRepository;

    @Mock
    private PhoneSubmissionOutbox phoneSubmissionOutbox;

    private SubmitPhoneUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new SubmitPhoneUseCaseImpl(patientDirectory, reservationProcessRepository, phoneSubmissionOutbox,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    // El proceso es de Juan (usuario 42), que es quien pide.
    private void juansProcessIs(ReservationProcess process) {
        when(patientDirectory.findPatient("juan.perez")).thenReturn(juan());
        when(reservationProcessRepository.findReservationProcessById(process.getId())).thenReturn(Optional.of(process));
    }

    @Test
    void thePhoneIsSavedInTheOutboxWithTheProcessChange() {
        ReservationProcess process = phoneRequested();
        juansProcessIs(process);
        when(reservationProcessRepository.saveReservationProcess(process)).thenReturn(process);

        ReservationProcess result = useCase.submitPhone("juan.perez", process.getId(), "+54 9 261 555-1234");

        assertThat(result.getStatus()).isEqualTo(ReservationStatus.PHONE_SUBMITTED);
        ArgumentCaptor<PhoneSubmission> saved = ArgumentCaptor.forClass(PhoneSubmission.class);
        verify(phoneSubmissionOutbox).savePhoneSubmission(saved.capture());
        assertThat(saved.getValue()).satisfies(submission -> {
            assertThat(submission.getEventId()).startsWith("phone-submitted-");
            assertThat(submission.getReservationProcessId()).isEqualTo(process.getId());
            assertThat(submission.getCatedraProcessId()).isEqualTo("process-a92b47d0");
            assertThat(submission.getHoldId()).isEqualTo("hold-6c0c2f41");
            assertThat(submission.getRequestEventId()).isEqualTo("request-event-8a31");
            assertThat(submission.getPhoneNumber()).isEqualTo("+5492615551234");
            assertThat(submission.getCreatedAt()).isEqualTo(NOW);
            assertThat(submission.getSentAt()).isNull();
            assertThat(submission.getAttempts()).isZero();
        });
    }

    @Test
    void anInvalidPhoneIsRejectedWithoutChangingAnything() {
        ReservationProcess process = phoneRequested();
        juansProcessIs(process);

        assertThatThrownBy(() -> useCase.submitPhone("juan.perez", process.getId(), "123"))
                .isInstanceOf(ReservationException.class)
                .extracting("code").isEqualTo(ReservationException.INVALID_PHONE_NUMBER);
        assertThat(process.getStatus()).isEqualTo(ReservationStatus.WAITING_FOR_PHONE);
        verify(phoneSubmissionOutbox, never()).savePhoneSubmission(any());
    }

    @Test
    void withoutARequestFromTheCatedraThePhoneIsNotAccepted() {
        ReservationProcess process = waitingForPhone();
        juansProcessIs(process);

        assertThatThrownBy(() -> useCase.submitPhone("juan.perez", process.getId(), "+5492615551234"))
                .isInstanceOf(ReservationException.class)
                .extracting("code").isEqualTo(ReservationException.PHONE_NOT_REQUESTED);
        verify(phoneSubmissionOutbox, never()).savePhoneSubmission(any());
    }

    @Test
    void theProcessOfAnotherUserIsNotFound() {
        ReservationProcess process = phoneRequested();
        when(reservationProcessRepository.findReservationProcessById(process.getId())).thenReturn(Optional.of(process));
        when(patientDirectory.findPatient("otro")).thenReturn(Patient.builder().userId(99L).build());

        assertThatThrownBy(() -> useCase.submitPhone("otro", process.getId(), "+5492615551234"))
                .isInstanceOf(ReservationException.class)
                .extracting("code").isEqualTo(ReservationException.RESERVATION_NOT_FOUND);
    }

    @Test
    void anUnknownProcessIsNotFound() {
        UUID id = UUID.randomUUID();
        when(patientDirectory.findPatient("juan.perez")).thenReturn(juan());
        when(reservationProcessRepository.findReservationProcessById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.submitPhone("juan.perez", id, "+5492615551234"))
                .isInstanceOf(ReservationException.class)
                .extracting("code").isEqualTo(ReservationException.RESERVATION_NOT_FOUND);
    }
}
