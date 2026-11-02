package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.adapter;

import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.NOW;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.phoneRequested;
import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.um.prog2.quiroga.turnos.TestcontainersConfiguration;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.PhoneSubmission;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.mapper.PhoneSubmissionMapper;
import ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.persistence.mapper.ReservationProcessMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaPhoneSubmissionOutboxAdapter.class, PhoneSubmissionMapper.class,
        JpaReservationProcessRepositoryAdapter.class, ReservationProcessMapper.class})
class JpaPhoneSubmissionOutboxAdapterTest {

    @Autowired
    private JpaPhoneSubmissionOutboxAdapter outbox;

    @Autowired
    private JpaReservationProcessRepositoryAdapter reservationProcessRepository;

    private ReservationProcess process;

    // Cada fila del outbox es de un proceso existente (clave foránea).
    @BeforeEach
    void saveProcess() {
        process = reservationProcessRepository.saveReservationProcess(phoneRequested());
    }

    private PhoneSubmission submission(int secondsAfterNow) {
        return PhoneSubmission.of(process, "+5492615551234", NOW.plusSeconds(secondsAfterNow));
    }

    @Test
    void unsentSubmissionsComeOldestFirst() {
        PhoneSubmission newer = submission(20);
        PhoneSubmission older = submission(10);
        outbox.savePhoneSubmission(newer);
        outbox.savePhoneSubmission(older);

        assertThat(outbox.findUnsentPhoneSubmissions(10, 5))
                .extracting(PhoneSubmission::getEventId)
                .containsExactly(older.getEventId(), newer.getEventId());
    }

    @Test
    void sentOnesAndThoseOutOfAttemptsAreLeftOut() {
        PhoneSubmission sent = submission(10);
        sent.markSent(NOW.plusSeconds(11));
        PhoneSubmission exhausted = submission(20);
        exhausted.setAttempts(10);
        PhoneSubmission pending = submission(30);
        pending.recordFailedAttempt();
        outbox.savePhoneSubmission(sent);
        outbox.savePhoneSubmission(exhausted);
        outbox.savePhoneSubmission(pending);

        assertThat(outbox.findUnsentPhoneSubmissions(10, 5)).singleElement().satisfies(found -> {
            assertThat(found.getEventId()).isEqualTo(pending.getEventId());
            assertThat(found.getAttempts()).isEqualTo(1);
            assertThat(found.getPhoneNumber()).isEqualTo("+5492615551234");
            assertThat(found.getRequestEventId()).isEqualTo("request-event-8a31");
        });
    }

    @Test
    void theLimitIsRespected() {
        outbox.savePhoneSubmission(submission(10));
        outbox.savePhoneSubmission(submission(20));

        assertThat(outbox.findUnsentPhoneSubmissions(10, 1)).hasSize(1);
    }
}
