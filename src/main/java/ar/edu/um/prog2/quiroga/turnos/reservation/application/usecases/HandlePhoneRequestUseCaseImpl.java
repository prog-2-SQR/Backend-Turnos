package ar.edu.um.prog2.quiroga.turnos.reservation.application.usecases;

import java.time.Clock;

import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.PhoneRequest;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in.HandlePhoneRequestUseCase;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.ProcessedEventRepository;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * AdditionalInformationRequested (enunciado §7 paso 4, anexo §15.4). El cambio del proceso y el registro del eventId
 * van en la misma transacción: o quedan los dos o ninguno, y un evento repetido no tiene efecto (anexo §18.1).
 * - Proceso desconocido: se registra el evento y se ignora.
 * - Proceso terminado: un evento tardío no reabre un estado final (anexo §18.3).
 * - Confirmación todavía sin guardar: el evento llegó antes que la respuesta REST; se lanza CONFIRMATION_IN_FLIGHT
 *   para que el listener lo reintente en un momento.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HandlePhoneRequestUseCaseImpl implements HandlePhoneRequestUseCase {

    static final String EVENT_TYPE = "AdditionalInformationRequested";

    private final ReservationProcessRepository reservationProcessRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final Clock clock;

    @Override
    @Transactional
    public void handlePhoneRequest(PhoneRequest request) {
        if (processedEventRepository.existsByEventId(request.getEventId())) {
            log.info("Pedido de teléfono {} ya procesado: se ignora", request.getEventId());
            return;
        }
        reservationProcessRepository.findReservationProcessByCatedraProcessId(request.getCatedraProcessId())
                .ifPresentOrElse(process -> apply(process, request),
                        () -> log.warn("Pedido de teléfono {} para un proceso desconocido: {}",
                                request.getEventId(), request.getCatedraProcessId()));
        processedEventRepository.saveProcessedEvent(request.getEventId(), EVENT_TYPE);
    }

    private void apply(ReservationProcess process, PhoneRequest request) {
        if (process.isConfirmationInFlight()) {
            log.info("Pedido de teléfono {} llegó antes de que se guardara la confirmación del proceso {}: se reintenta",
                    request.getEventId(), process.getId());
            throw new ReservationException(ReservationException.CONFIRMATION_IN_FLIGHT,
                    "El proceso " + process.getId() + " todavía no guardó la confirmación del hold");
        }
        if (!process.isPending()) {
            log.info("Pedido de teléfono {} para el proceso {}, que ya terminó en {}: se ignora",
                    request.getEventId(), process.getId(), process.getStatus());
            return;
        }
        process.receivePhoneRequest(request, clock.instant());
        reservationProcessRepository.saveReservationProcess(process);
        log.info("La cátedra pidió el teléfono del proceso {}", process.getId());
    }
}
