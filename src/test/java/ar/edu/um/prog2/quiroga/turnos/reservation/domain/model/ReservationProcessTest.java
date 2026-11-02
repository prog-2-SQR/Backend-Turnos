package ar.edu.um.prog2.quiroga.turnos.reservation.domain.model;

import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.HOLD_EXPIRES_AT;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.MONDAY;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.NOW;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.TWELVE_THIRTY;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.held;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.hold;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.phoneRequest;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.phoneRequested;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.phoneSubmitted;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.result;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.waitingForPhone;
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
    void aPhoneRequestIsKeptToAnswerIt() {
        ReservationProcess process = waitingForPhone();
        assertThat(process.isPhoneRequested()).isFalse();

        process.receivePhoneRequest(phoneRequest(), NOW);

        assertThat(process.getStatus()).isEqualTo(ReservationStatus.WAITING_FOR_PHONE);
        assertThat(process.getPhoneRequestEventId()).isEqualTo("request-event-8a31");
        assertThat(process.getPhoneRequestMessage()).isEqualTo("Ingrese un numero de telefono para continuar.");
        assertThat(process.getExpiresAt()).isEqualTo(HOLD_EXPIRES_AT.plusSeconds(300));
        assertThat(process.isPhoneRequested()).isTrue();
    }

    @Test
    void aNewPhoneRequestAfterSendingOneWaitsAgain() {
        ReservationProcess process = waitingForPhone();
        process.setStatus(ReservationStatus.PHONE_SUBMITTED);

        process.receivePhoneRequest(phoneRequest(), NOW);

        assertThat(process.getStatus()).isEqualTo(ReservationStatus.WAITING_FOR_PHONE);
    }

    @Test
    void theConfirmationIsInFlightUntilItIsSaved() {
        assertThat(requested().isConfirmationInFlight()).isTrue();
        assertThat(held().isConfirmationInFlight()).isTrue();
        assertThat(waitingForPhone().isConfirmationInFlight()).isFalse();
    }

    @Test
    void belongsOnlyToItsUser() {
        assertThat(requested().belongsTo(42L)).isTrue();
        assertThat(requested().belongsTo(7L)).isFalse();
    }

    @Test
    void submittingThePhoneWaitsForTheResult() {
        ReservationProcess process = phoneRequested();

        process.submitPhone(NOW.plusSeconds(30));

        assertThat(process.getStatus()).isEqualTo(ReservationStatus.PHONE_SUBMITTED);
        assertThat(process.isPhoneRequested()).isFalse();
        assertThat(process.getUpdatedAt()).isEqualTo(NOW.plusSeconds(30));
    }

    @Test
    void thePhoneCannotBeSubmittedBeforeTheCatedraAsksForIt() {
        ReservationProcess process = waitingForPhone();

        assertThatThrownBy(() -> process.submitPhone(NOW)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void aConfirmationKeepsTheReservationIdOfTheCatedra() {
        ReservationProcess process = phoneSubmitted();
        ReservationResult confirmed = result(ReservationResultType.CONFIRMED);
        confirmed.setCatedraReservationId(8251L);

        assertThat(process.canApply(confirmed)).isTrue();
        process.applyResult(confirmed, NOW.plusSeconds(60));

        assertThat(process.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(process.getCatedraReservationId()).isEqualTo(8251L);
        assertThat(process.isPending()).isFalse();
    }

    @Test
    void aRejectedPhoneCanBeSentAgainUntilTheNewExpiration() {
        ReservationProcess process = phoneSubmitted();
        ReservationResult rejected = result(ReservationResultType.PHONE_REJECTED);
        rejected.setMessage("El telefono ingresado no es valido.");
        rejected.setExpiresAt(HOLD_EXPIRES_AT.plusSeconds(120));

        process.applyResult(rejected, NOW.plusSeconds(60));

        assertThat(process.getStatus()).isEqualTo(ReservationStatus.WAITING_FOR_PHONE);
        assertThat(process.isPhoneRequested()).isTrue();
        assertThat(process.getPhoneRejectionMessage()).isEqualTo("El telefono ingresado no es valido.");
        assertThat(process.getExpiresAt()).isEqualTo(HOLD_EXPIRES_AT.plusSeconds(120));

        process.submitPhone(NOW.plusSeconds(90));
        assertThat(process.getPhoneRejectionMessage()).isNull();
    }

    @Test
    void expiredAndInvalidKeepTheReason() {
        ReservationProcess expired = phoneSubmitted();
        ReservationResult expiration = result(ReservationResultType.EXPIRED);
        expiration.setReason("HOLD_EXPIRED");
        expired.applyResult(expiration, NOW);

        ReservationProcess invalid = phoneSubmitted();
        ReservationResult invalidation = result(ReservationResultType.INVALID);
        invalidation.setReason("x".repeat(80));
        invalid.applyResult(invalidation, NOW);

        assertThat(expired.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(expired.getFailureCode()).isEqualTo("HOLD_EXPIRED");
        assertThat(invalid.getStatus()).isEqualTo(ReservationStatus.INVALID);
        assertThat(invalid.getFailureCode()).hasSize(60);
    }

    @Test
    void onlyAConfirmedReservationCanBeCancelled() {
        ReservationProcess confirmed = phoneSubmitted();
        confirmed.applyResult(result(ReservationResultType.CONFIRMED), NOW);

        assertThat(phoneSubmitted().canApply(result(ReservationResultType.CANCELLED))).isFalse();
        assertThat(confirmed.canApply(result(ReservationResultType.CANCELLED))).isTrue();
        confirmed.applyResult(result(ReservationResultType.CANCELLED), NOW);
        assertThat(confirmed.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void aLateResultDoesNotReopenAFinalState() {
        ReservationProcess confirmed = phoneSubmitted();
        confirmed.applyResult(result(ReservationResultType.CONFIRMED), NOW);

        assertThat(confirmed.canApply(result(ReservationResultType.EXPIRED))).isFalse();
        assertThat(confirmed.canApply(result(ReservationResultType.PHONE_REJECTED))).isFalse();
        assertThatThrownBy(() -> confirmed.applyResult(result(ReservationResultType.EXPIRED), NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void onlyAConfirmedReservationIsCancellableByTheUser() {
        ReservationProcess process = phoneSubmitted();
        assertThat(process.isCancellable()).isFalse();

        process.applyResult(result(ReservationResultType.CONFIRMED), NOW);
        assertThat(process.isCancellable()).isTrue();

        process.cancel(NOW.plusSeconds(60));
        assertThat(process.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(process.isCancellable()).isFalse();
    }

    private static CatedraReservation inCatedra(CatedraReservationStatus status) {
        return CatedraReservation.builder().catedraProcessId("process-a92b47d0").catedraReservationId(1258L).status(status).build();
    }

    @Test
    void aProcessFailedByTheCatedraWithoutAnEventIsReconciled() {
        ReservationProcess process = waitingForPhone();

        assertThat(process.reconcileWith(inCatedra(CatedraReservationStatus.FAILED), NOW)).isTrue();

        assertThat(process.getStatus()).isEqualTo(ReservationStatus.FAILED);
        assertThat(process.getFailureCode()).isEqualTo("CATEDRA_FAILED");
    }

    @Test
    void aConfirmationWhoseEventWasLostIsReconciled() {
        ReservationProcess process = phoneSubmitted();

        assertThat(process.reconcileWith(inCatedra(CatedraReservationStatus.CONFIRMED), NOW)).isTrue();

        assertThat(process.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(process.getCatedraReservationId()).isEqualTo(1258L);
    }

    @Test
    void aHoldConfirmedWhoseResponseWasLostWaitsForThePhone() {
        ReservationProcess process = held();

        assertThat(process.reconcileWith(inCatedra(CatedraReservationStatus.PHONE_PENDING), NOW)).isTrue();

        assertThat(process.getStatus()).isEqualTo(ReservationStatus.WAITING_FOR_PHONE);
    }

    @Test
    void theSameStatusOrAnUnknownOneChangesNothing() {
        assertThat(waitingForPhone().reconcileWith(inCatedra(CatedraReservationStatus.PHONE_PENDING), NOW)).isFalse();
        assertThat(waitingForPhone().reconcileWith(inCatedra(null), NOW)).isFalse();
    }

    @Test
    void aFinalStatusDoesNotGoBackWithTheReconciliation() {
        ReservationProcess process = phoneSubmitted();
        process.applyResult(result(ReservationResultType.CONFIRMED), NOW);

        assertThat(process.reconcileWith(inCatedra(CatedraReservationStatus.FAILED), NOW)).isFalse();
        assertThat(process.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }
}
