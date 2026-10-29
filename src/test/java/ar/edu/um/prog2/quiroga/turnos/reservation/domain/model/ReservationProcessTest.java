package ar.edu.um.prog2.quiroga.turnos.reservation.domain.model;

import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.HOLD_EXPIRES_AT;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.MONDAY;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.NOW;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.TWELVE_THIRTY;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.held;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.hold;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.requested;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class ReservationProcessTest {

    @Test
    void aNewProcessIsRequestedForTheSlotAndThePatient() {
        ReservationProcess process = requested();

        assertThat(process.getId()).isNotNull();
        assertThat(process.getStatus()).isEqualTo(ReservationStatus.REQUESTED);
        assertThat(process.getUserId()).isEqualTo(42L);
        assertThat(process.getExternalPatientId()).isEqualTo("user-42");
        assertThat(process.getProfessionalId()).isEqualTo(201L);
        assertThat(process.getDate()).isEqualTo(MONDAY);
        assertThat(process.getStartTime()).isEqualTo(TWELVE_THIRTY);
        assertThat(process.isPending()).isTrue();
    }

    @Test
    void theHoldKeepsTheIdsOfTheCatedraAndItsExpiration() {
        ReservationProcess process = held();

        assertThat(process.getStatus()).isEqualTo(ReservationStatus.HELD);
        assertThat(process.getHoldId()).isEqualTo("hold-6c0c2f41");
        assertThat(process.getCatedraProcessId()).isEqualTo("process-a92b47d0");
        assertThat(process.getExpiresAt()).isEqualTo(HOLD_EXPIRES_AT);
    }

    @Test
    void confirmingTheHoldWaitsForThePhoneWithTheNewExpiration() {
        ReservationProcess process = held();
        Instant later = HOLD_EXPIRES_AT.plusSeconds(60);

        process.waitForPhone(later, NOW.plusSeconds(5));

        assertThat(process.getStatus()).isEqualTo(ReservationStatus.WAITING_FOR_PHONE);
        assertThat(process.getExpiresAt()).isEqualTo(later);
        assertThat(process.getUpdatedAt()).isEqualTo(NOW.plusSeconds(5));
    }

    @Test
    void withoutANewExpirationTheHoldOneIsKept() {
        ReservationProcess process = held();

        process.waitForPhone(null, NOW);

        assertThat(process.getExpiresAt()).isEqualTo(HOLD_EXPIRES_AT);
    }

    @Test
    void aFailureKeepsItsCodeAndEndsTheProcess() {
        ReservationProcess process = requested();

        process.fail("SLOT_ALREADY_HELD", NOW);

        assertThat(process.getStatus()).isEqualTo(ReservationStatus.FAILED);
        assertThat(process.getFailureCode()).isEqualTo("SLOT_ALREADY_HELD");
        assertThat(process.isPending()).isFalse();
    }

    @Test
    void aStepThatIsNotAllowedIsAnError() {
        ReservationProcess process = requested();
        process.fail("SLOT_ALREADY_HELD", NOW);

        assertThatThrownBy(() -> process.hold(hold(), NOW)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aHoldIsAbandonedWhenItExpires() {
        ReservationProcess process = held();

        assertThat(process.isAbandoned(HOLD_EXPIRES_AT.minusSeconds(1))).isFalse();
        assertThat(process.isAbandoned(HOLD_EXPIRES_AT)).isTrue();

        process.close(HOLD_EXPIRES_AT);
        assertThat(process.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
    }

    @Test
    void aRequestWithoutAnswerIsAbandonedAfterAWhile() {
        ReservationProcess process = requested();

        assertThat(process.isAbandoned(NOW.plus(ReservationProcess.REQUEST_TIMEOUT).minusSeconds(1))).isFalse();
        assertThat(process.isAbandoned(NOW.plus(ReservationProcess.REQUEST_TIMEOUT))).isTrue();

        process.close(NOW.plus(ReservationProcess.REQUEST_TIMEOUT));
        assertThat(process.getStatus()).isEqualTo(ReservationStatus.FAILED);
        assertThat(process.getFailureCode()).isEqualTo(ReservationProcess.ABANDONED);
    }

    @Test
    void belongsOnlyToItsUser() {
        assertThat(requested().belongsTo(42L)).isTrue();
        assertThat(requested().belongsTo(7L)).isFalse();
    }
}
