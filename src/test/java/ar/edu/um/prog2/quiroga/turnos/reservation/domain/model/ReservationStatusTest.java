package ar.edu.um.prog2.quiroga.turnos.reservation.domain.model;

import static ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationStatus.CANCELLED;
import static ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationStatus.CONFIRMED;
import static ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationStatus.EXPIRED;
import static ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationStatus.FAILED;
import static ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationStatus.HELD;
import static ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationStatus.INVALID;
import static ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationStatus.PHONE_SUBMITTED;
import static ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationStatus.REQUESTED;
import static ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationStatus.WAITING_FOR_PHONE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ReservationStatusTest {

    @Test
    void theHappyPathMovesForward() {
        assertThat(REQUESTED.canMoveTo(HELD)).isTrue();
        assertThat(HELD.canMoveTo(WAITING_FOR_PHONE)).isTrue();
        assertThat(WAITING_FOR_PHONE.canMoveTo(PHONE_SUBMITTED)).isTrue();
        assertThat(PHONE_SUBMITTED.canMoveTo(CONFIRMED)).isTrue();
        assertThat(CONFIRMED.canMoveTo(CANCELLED)).isTrue();
    }

    @Test
    void aRejectedPhoneCanBeSentAgain() {
        assertThat(PHONE_SUBMITTED.canMoveTo(WAITING_FOR_PHONE)).isTrue();
    }

    @Test
    void neverGoesBack() {
        assertThat(HELD.canMoveTo(REQUESTED)).isFalse();
        assertThat(WAITING_FOR_PHONE.canMoveTo(HELD)).isFalse();
        assertThat(CONFIRMED.canMoveTo(PHONE_SUBMITTED)).isFalse();
        assertThat(CONFIRMED.canMoveTo(EXPIRED)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(names = {"FAILED", "EXPIRED", "INVALID", "CANCELLED"})
    void aFinalStatusDoesNotChangeAnymore(ReservationStatus finalStatus) {
        assertThat(EnumSet.allOf(ReservationStatus.class)).noneMatch(finalStatus::canMoveTo);
        assertThat(finalStatus.isPending()).isFalse();
    }

    @Test
    void onlyTheUnfinishedStatusesArePending() {
        assertThat(EnumSet.allOf(ReservationStatus.class)).filteredOn(ReservationStatus::isPending)
                .containsExactlyInAnyOrder(REQUESTED, HELD, WAITING_FOR_PHONE, PHONE_SUBMITTED);
        assertThat(CONFIRMED.isPending()).isFalse();
        assertThat(INVALID.isPending()).isFalse();
        assertThat(FAILED.isPending()).isFalse();
    }
}
