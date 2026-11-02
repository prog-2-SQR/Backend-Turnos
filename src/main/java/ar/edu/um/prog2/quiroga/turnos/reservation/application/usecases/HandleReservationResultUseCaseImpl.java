package ar.edu.um.prog2.quiroga.turnos.reservation.application.usecases;

import java.time.Clock;

import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationResult;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in.HandleReservationResultUseCase;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.ProcessedEventRepository;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resultados del proceso (enunciado §7 pasos 6 y 7; anexo §15.6 a §15.10). Mismo criterio que el pedido de teléfono:
 * el cambio del proceso y el registro del eventId van en la misma transacción, y un evento repetido no tiene efecto
 * (anexo §18.1).
 * - Proceso desconocido: se registra el evento y se ignora.
 * - Transición permitida: se aplica.
 * - Confirmación del hold todavía sin guardar: se lanza CONFIRMATION_IN_FLIGHT para que el listener lo reintente.
 * - Cualquier otra (por ejemplo, un vencimiento después de la confirmación): se ignora. Un estado final no retrocede
 *   (anexo §18.3).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HandleReservationResultUseCaseImpl implements HandleReservationResultUseCase {

    private final ReservationProcessRepository reservationProcessRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final Clock clock;

    @Override
    @Transactional
    public void handleReservationResult(ReservationResult result) {
        if (processedEventRepository.existsByEventId(result.getEventId())) {
            log.info("Resultado {} ya procesado: se ignora", result.getEventId());
            return;
        }
        reservationProcessRepository.findReservationProcessByCatedraProcessId(result.getCatedraProcessId())
                .ifPresentOrElse(process -> apply(process, result),
                        () -> log.warn("Resultado {} ({}) para un proceso desconocido: {}",
                                result.getEventId(), result.getType(), result.getCatedraProcessId()));
        processedEventRepository.saveProcessedEvent(result.getEventId(), result.getType().name());
    }

    private void apply(ReservationProcess process, ReservationResult result) {
        if (process.canApply(result)) {
            process.applyResult(result, clock.instant());
            reservationProcessRepository.saveReservationProcess(process);
            log.info("Proceso {}: {} → {}", process.getId(), result.getType(), process.getStatus());
            return;
        }
        if (process.isConfirmationInFlight()) {
            log.info("Resultado {} llegó antes de que se guardara la confirmación del proceso {}: se reintenta",
                    result.getEventId(), process.getId());
            throw new ReservationException(ReservationException.CONFIRMATION_IN_FLIGHT,
                    "El proceso " + process.getId() + " todavía no guardó la confirmación del hold");
        }
        log.info("Resultado {} ({}) para el proceso {} en {}: no corresponde, se ignora",
                result.getEventId(), result.getType(), process.getId(), process.getStatus());
    }
}
