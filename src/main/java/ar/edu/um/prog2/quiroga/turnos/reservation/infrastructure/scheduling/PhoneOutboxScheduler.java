package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.scheduling;

import ar.edu.um.prog2.quiroga.turnos.config.PhoneOutboxProperties;
import ar.edu.um.prog2.quiroga.turnos.reservation.application.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cada pocos segundos publica los teléfonos pendientes del outbox. fixedDelay: la próxima vuelta empieza cuando
 * termina la anterior, así dos vueltas nunca envían la misma fila a la vez.
 */
@Component
@RequiredArgsConstructor
public class PhoneOutboxScheduler {

    private final ReservationService reservationService;
    private final PhoneOutboxProperties phoneOutboxProperties;

    @Scheduled(initialDelayString = "${catedra.phone-outbox.interval:PT2S}", fixedDelayString = "${catedra.phone-outbox.interval:PT2S}")
    public void publishPendingPhones() {
        reservationService.publishPendingPhoneSubmissions(phoneOutboxProperties.getMaxAttempts());
    }
}
