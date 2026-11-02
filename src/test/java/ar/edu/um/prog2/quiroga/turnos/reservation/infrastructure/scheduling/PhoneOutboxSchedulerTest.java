package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.scheduling;

import static org.mockito.Mockito.verify;

import ar.edu.um.prog2.quiroga.turnos.config.PhoneOutboxProperties;
import ar.edu.um.prog2.quiroga.turnos.reservation.application.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PhoneOutboxSchedulerTest {

    @Mock
    private ReservationService reservationService;

    @Test
    void publishesWithTheConfiguredAttemptLimit() {
        PhoneOutboxProperties properties = new PhoneOutboxProperties();
        properties.setMaxAttempts(7);

        new PhoneOutboxScheduler(reservationService, properties).publishPendingPhones();

        verify(reservationService).publishPendingPhoneSubmissions(7);
    }
}
