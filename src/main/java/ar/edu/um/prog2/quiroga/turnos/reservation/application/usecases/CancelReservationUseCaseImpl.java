package ar.edu.um.prog2.quiroga.turnos.reservation.application.usecases;

import java.time.Clock;
import java.util.UUID;

import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationStatus;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in.CancelReservationUseCase;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.CatedraReservationClient;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.PatientDirectory;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Cancela una reserva propia confirmada (enunciado §5 punto 14; anexo §12).
 * - Primero en la cátedra y después en Turnos: si la cátedra no la cancela, Turnos no la marca como cancelada.
 * - Una ya cancelada se devuelve igual (idempotente, como en la cátedra). Otro estado → RESERVATION_NOT_CANCELLABLE.
 * - La cátedra también publica AppointmentCancelled: si el listener la guardó primero, acá se devuelve tal como quedó.
 * No hay transacción abierta mientras se espera a la cátedra.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CancelReservationUseCaseImpl implements CancelReservationUseCase {

    private final PatientDirectory patientDirectory;
    private final ReservationProcessRepository reservationProcessRepository;
    private final CatedraReservationClient catedraReservationClient;
    private final Clock clock;

    @Override
    public ReservationProcess cancelReservation(String login, UUID reservationId, String reason) {
        ReservationProcess process = ownProcess(login, reservationId);
        if (process.getStatus() == ReservationStatus.CANCELLED) {
            return process;
        }
        if (!process.isCancellable()) {
            throw new ReservationException(ReservationException.RESERVATION_NOT_CANCELLABLE,
                    "Solo se puede cancelar una reserva confirmada");
        }
        catedraReservationClient.cancelReservation(process.getCatedraProcessId(), reason);
        ReservationProcess current = reservationProcessRepository.findReservationProcessById(reservationId).orElseThrow();
        if (current.isCancellable()) {
            current.cancel(clock.instant());
            current = reservationProcessRepository.saveReservationProcess(current);
        }
        log.info("Reserva {} cancelada", reservationId);
        return current;
    }

    // Aislamiento entre usuarios (enunciado §4.2): un proceso ajeno se trata igual que uno inexistente.
    private ReservationProcess ownProcess(String login, UUID reservationId) {
        Long userId = patientDirectory.findPatient(login).getUserId();
        return reservationProcessRepository.findReservationProcessById(reservationId)
                .filter(process -> process.belongsTo(userId))
                .orElseThrow(() -> new ReservationException(ReservationException.RESERVATION_NOT_FOUND,
                        "No existe la reserva " + reservationId));
    }
}
