package ar.edu.um.prog2.quiroga.turnos.reservation.application.usecases;

import java.time.Clock;
import java.time.Instant;

import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.Patient;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationRequest;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in.StartReservationUseCase;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.HoldClient;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.PatientDirectory;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.SlotAvailability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Inicia una reserva (enunciado §7, pasos 1 a 3; anexo §9 y §10):
 * 1. valida al paciente y que el usuario no tenga otra reserva en curso;
 * 2. revalida el turno con la información vigente (catálogo y ocupaciones);
 * 3. guarda el proceso ANTES de llamar a la cátedra (anexo §18.4: el progreso queda persistido);
 * 4. pide el hold y lo confirma: el proceso queda esperando el teléfono.
 * Si la cátedra rechaza algo, el proceso se cierra (FAILED o EXPIRED) y el error llega al cliente con su code.
 * Cada guardado es su propia transacción: no se tiene una conexión a la base abierta mientras se espera a la cátedra.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartReservationUseCaseImpl implements StartReservationUseCase {

    private final PatientDirectory patientDirectory;
    private final SlotAvailability slotAvailability;
    private final ReservationProcessRepository reservationProcessRepository;
    private final HoldClient holdClient;
    private final Clock clock;

    @Override
    public ReservationProcess startReservation(String login, ReservationRequest request) {
        Patient patient = patientDirectory.findPatient(login).withNames(request.getPatientFirstName(), request.getPatientLastName());
        if (!patient.hasValidName()) {
            throw new ReservationException(ReservationException.PATIENT_NAME_REQUIRED,
                    "La cátedra necesita nombre y apellido del paciente (2 a 100 caracteres)");
        }
        closeAbandonedPending(patient.getUserId());
        if (!slotAvailability.isAvailable(request.getProfessionalId(), request.getDate(), request.getStartTime())) {
            throw new ReservationException(ReservationException.SLOT_NOT_AVAILABLE, "El turno ya no está disponible");
        }
        ReservationProcess process = reservationProcessRepository.saveReservationProcess(
                ReservationProcess.request(patient, request, clock.instant()));
        return confirm(hold(process), patient);
    }

    // Un pendiente vigente impide empezar otro. Si ya no puede seguir (hold vencido, pedido sin respuesta), se cierra.
    private void closeAbandonedPending(Long userId) {
        Instant now = clock.instant();
        reservationProcessRepository.findPendingReservationProcess(userId).ifPresent(pending -> {
            if (!pending.isAbandoned(now)) {
                throw new ReservationException(ReservationException.RESERVATION_ALREADY_PENDING,
                        "El usuario ya tiene una reserva en curso");
            }
            log.info("Proceso {} abandonado en {}: se cierra", pending.getId(), pending.getStatus());
            pending.close(now);
            reservationProcessRepository.saveReservationProcess(pending);
        });
    }

    private ReservationProcess hold(ReservationProcess process) {
        try {
            process.hold(holdClient.createHold(process), clock.instant());
            return reservationProcessRepository.saveReservationProcess(process);
        } catch (ReservationException e) {
            process.fail(e.getCode(), clock.instant());
            reservationProcessRepository.saveReservationProcess(process);
            throw e;
        }
    }

    private ReservationProcess confirm(ReservationProcess process, Patient patient) {
        try {
            process.waitForPhone(holdClient.confirmHold(process, patient), clock.instant());
            return reservationProcessRepository.saveReservationProcess(process);
        } catch (ReservationException e) {
            closeAfterFailedConfirmation(process, e.getCode());
            throw e;
        }
    }

    // Hold vencido → EXPIRED (anexo §18.3); otro rechazo → FAILED. El hold que haya quedado vence solo en la cátedra.
    private void closeAfterFailedConfirmation(ReservationProcess process, String code) {
        if (ReservationException.HOLD_EXPIRED.equals(code)) {
            process.expire(clock.instant());
        } else {
            process.fail(code, clock.instant());
        }
        reservationProcessRepository.saveReservationProcess(process);
    }
}
