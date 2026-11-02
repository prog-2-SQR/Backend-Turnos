package ar.edu.um.prog2.quiroga.turnos.reservation.application.usecases;

import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.NOW;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.phoneRequested;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;

import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.PhoneSubmission;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.PhoneSubmissionOutbox;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.PhoneSubmissionPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublishPhoneSubmissionsUseCaseImplTest {

    private static final int MAX_ATTEMPTS = 3;

    @Mock
    private PhoneSubmissionOutbox phoneSubmissionOutbox;

    @Mock
    private PhoneSubmissionPublisher phoneSubmissionPublisher;

    private PublishPhoneSubmissionsUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new PublishPhoneSubmissionsUseCaseImpl(phoneSubmissionOutbox, phoneSubmissionPublisher,
                Clock.fixed(NOW.plusSeconds(5), ZoneOffset.UTC));
    }

    private PhoneSubmission pending() {
        PhoneSubmission submission = PhoneSubmission.of(phoneRequested(), "+5492615551234", NOW);
        when(phoneSubmissionOutbox.findUnsentPhoneSubmissions(MAX_ATTEMPTS, PublishPhoneSubmissionsUseCaseImpl.BATCH_SIZE))
                .thenReturn(List.of(submission));
        return submission;
    }

    @Test
    void aPublishedPhoneIsMarkedAsSent() {
        PhoneSubmission submission = pending();

        useCase.publishPendingPhoneSubmissions(MAX_ATTEMPTS);

        verify(phoneSubmissionPublisher).publish(submission);
        verify(phoneSubmissionOutbox).savePhoneSubmission(submission);
        assertThat(submission.getSentAt()).isEqualTo(NOW.plusSeconds(5));
        assertThat(submission.getAttempts()).isZero();
    }

    @Test
    void aFailedSendCountsAnAttemptAndKeepsTheSameEventId() {
        PhoneSubmission submission = pending();
        String eventId = submission.getEventId();
        doThrow(new ReservationException(ReservationException.CATEDRA_UNAVAILABLE, "Kafka caído"))
                .when(phoneSubmissionPublisher).publish(submission);

        useCase.publishPendingPhoneSubmissions(MAX_ATTEMPTS);

        verify(phoneSubmissionOutbox).savePhoneSubmission(submission);
        assertThat(submission.getSentAt()).isNull();
        assertThat(submission.getAttempts()).isEqualTo(1);
        assertThat(submission.getEventId()).isEqualTo(eventId);
    }

    @Test
    void theLastFailedAttemptIsAlsoSaved() {
        PhoneSubmission submission = pending();
        submission.setAttempts(MAX_ATTEMPTS - 1);
        doThrow(new ReservationException(ReservationException.CATEDRA_UNAVAILABLE, "Kafka caído"))
                .when(phoneSubmissionPublisher).publish(submission);

        useCase.publishPendingPhoneSubmissions(MAX_ATTEMPTS);

        verify(phoneSubmissionOutbox).savePhoneSubmission(submission);
        assertThat(submission.getAttempts()).isEqualTo(MAX_ATTEMPTS);
    }

    @Test
    void withNothingPendingNothingIsSent() {
        when(phoneSubmissionOutbox.findUnsentPhoneSubmissions(anyInt(), anyInt())).thenReturn(List.of());

        useCase.publishPendingPhoneSubmissions(MAX_ATTEMPTS);

        verify(phoneSubmissionPublisher, never()).publish(any());
    }
}
