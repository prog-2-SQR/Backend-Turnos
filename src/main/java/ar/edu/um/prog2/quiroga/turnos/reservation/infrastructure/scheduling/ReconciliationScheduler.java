package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.scheduling;

import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import ar.edu.um.prog2.quiroga.turnos.reservation.application.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Reconcilia los procesos en curso con la cátedra (anexo §18.4): la primera vez poco después de arrancar (recupera lo
 * que quedó a medias antes de un reinicio) y después cada catedra.reconciliation-interval. Si la cátedra no responde,
 * se reintenta en la próxima vuelta.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReconciliationScheduler {

    private final ReservationService reservationService;

    @Scheduled(initialDelayString = "${catedra.reconciliation-initial-delay:PT15S}", fixedDelayString = "${catedra.reconciliation-interval:PT5M}")
    public void reconcile() {
        try {
            reservationService.reconcilePendingReservations();
        } catch (ReservationException e) {
            log.warn("No se pudo reconciliar con la cátedra ({}): se reintenta en la próxima vuelta", e.getCode());
        }
    }
}
