package ar.edu.um.prog2.quiroga.turnos.reservation.application.usecases;

import java.time.Clock;

import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.PhoneSubmission;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in.PublishPhoneSubmissionsUseCase;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.PhoneSubmissionOutbox;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.PhoneSubmissionPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Publica los teléfonos del outbox (anexo §15.5). Cuando Kafka confirma la recepción, la fila queda marcada como
 * enviada; eso no es la confirmación del turno, que llega después con AppointmentConfirmed.
 * Si el envío falla se suma un intento y se vuelve a probar en la próxima vuelta con el MISMO eventId: si el primer
 * envío había llegado, la cátedra ignora el duplicado. Los intentos tienen un límite (anexo §18.4: reintentos
 * limitados y observables); al llegar, el teléfono queda sin enviar y el proceso vence con su hold.
 * No hay transacción abierta mientras se espera a Kafka: cada fila se guarda por separado.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PublishPhoneSubmissionsUseCaseImpl implements PublishPhoneSubmissionsUseCase {

    static final int BATCH_SIZE = 20;

    private final PhoneSubmissionOutbox phoneSubmissionOutbox;
    private final PhoneSubmissionPublisher phoneSubmissionPublisher;
    private final Clock clock;

    @Override
    public void publishPendingPhoneSubmissions(int maxAttempts) {
        phoneSubmissionOutbox.findUnsentPhoneSubmissions(maxAttempts, BATCH_SIZE)
                .forEach(submission -> publish(submission, maxAttempts));
    }

    private void publish(PhoneSubmission submission, int maxAttempts) {
        try {
            phoneSubmissionPublisher.publish(submission);
            submission.markSent(clock.instant());
            log.info("Teléfono {} del proceso {} enviado a la cátedra", submission.getEventId(), submission.getReservationProcessId());
        } catch (ReservationException e) {
            submission.recordFailedAttempt();
            logFailure(submission, maxAttempts);
        }
        phoneSubmissionOutbox.savePhoneSubmission(submission);
    }

    private void logFailure(PhoneSubmission submission, int maxAttempts) {
        if (submission.getAttempts() >= maxAttempts) {
            log.error("No se pudo enviar el teléfono {} del proceso {} después de {} intentos: no se reintenta más",
                    submission.getEventId(), submission.getReservationProcessId(), submission.getAttempts());
        } else {
            log.warn("No se pudo enviar el teléfono {} del proceso {} (intento {}): se reintenta con el mismo eventId",
                    submission.getEventId(), submission.getReservationProcessId(), submission.getAttempts());
        }
    }
}
