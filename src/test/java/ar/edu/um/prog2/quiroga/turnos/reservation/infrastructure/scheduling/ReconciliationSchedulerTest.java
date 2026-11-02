package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.scheduling;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import ar.edu.um.prog2.quiroga.turnos.reservation.application.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReconciliationSchedulerTest {

    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private ReconciliationScheduler scheduler;

    @Test
    void reconcilesThePendingReservations() {
        scheduler.reconcile();

        verify(reservationService).reconcilePendingReservations();
    }

    @Test
    void aCatedraThatDoesNotAnswerDoesNotBreakTheScheduler() {
        when(reservationService.reconcilePendingReservations())
                .thenThrow(new ReservationException(ReservationException.CATEDRA_UNAVAILABLE, "caída"));

        assertThatCode(scheduler::reconcile).doesNotThrowAnyException();
    }
}
