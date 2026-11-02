package ar.edu.um.prog2.quiroga.turnos.reservation.application.usecases;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.PhoneNumber;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.PhoneSubmission;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in.SubmitPhoneUseCase;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.PatientDirectory;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.PhoneSubmissionOutbox;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * El usuario responde el pedido de teléfono (enunciado §7 paso 5). Patrón outbox: el cambio del proceso a
 * PHONE_SUBMITTED y el teléfono para enviar se guardan en la misma transacción, o quedan los dos o ninguno. Kafka no se
 * toca acá: si estuviera caído, el teléfono aceptado igual queda guardado y se publica después.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubmitPhoneUseCaseImpl implements SubmitPhoneUseCase {

    private final PatientDirectory patientDirectory;
    private final ReservationProcessRepository reservationProcessRepository;
    private final PhoneSubmissionOutbox phoneSubmissionOutbox;
    private final Clock clock;

    @Override
    @Transactional
    public ReservationProcess submitPhone(String login, UUID reservationId, String phoneNumber) {
        ReservationProcess process = ownProcess(login, reservationId);
        if (!process.isPhoneRequested()) {
            throw new ReservationException(ReservationException.PHONE_NOT_REQUESTED,
                    "La reserva no está esperando un teléfono");
        }
        String normalized = PhoneNumber.normalize(phoneNumber).orElseThrow(() -> new ReservationException(
                ReservationException.INVALID_PHONE_NUMBER, "El teléfono debe tener entre 7 y 15 dígitos, con + opcional"));
        Instant now = clock.instant();
        process.submitPhone(now);
        ReservationProcess saved = reservationProcessRepository.saveReservationProcess(process);
        phoneSubmissionOutbox.savePhoneSubmission(PhoneSubmission.of(saved, normalized, now));
        // El teléfono no va al log: es un dato personal.
        log.info("Teléfono del proceso {} guardado para enviar a la cátedra", saved.getId());
        return saved;
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
