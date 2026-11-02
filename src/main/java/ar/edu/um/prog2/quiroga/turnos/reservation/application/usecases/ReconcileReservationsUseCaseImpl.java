package ar.edu.um.prog2.quiroga.turnos.reservation.application.usecases;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.CatedraReservation;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in.ReconcileReservationsUseCase;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.CatedraReservationClient;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Recuperación de procesos en curso (enunciado §8; anexo §11 y §18.4): si se perdió una respuesta REST, se reinició
 * Turnos o la cátedra no avisó por Kafka, el estado local se pone al día con GET /api/appointments.
 * - Una consulta por paciente, filtrando por externalPatientId: el filtro por fechas (from/to) da 500 en la cátedra
 *   (probado el 02/11, Documentacion/integracion/pruebas-con-la-catedra.md).
 * - Cada proceso cambia solo si la máquina de estados lo permite (ReservationProcess.reconcileWith).
 * - Si la cátedra no responde, el error sube y se reintenta en la próxima vuelta.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReconcileReservationsUseCaseImpl implements ReconcileReservationsUseCase {

    private final ReservationProcessRepository reservationProcessRepository;
    private final CatedraReservationClient catedraReservationClient;
    private final Clock clock;

    @Override
    public int reconcilePendingReservations() {
        Map<String, List<ReservationProcess>> byPatient = reservationProcessRepository.findReconcilableReservationProcesses()
                .stream().collect(Collectors.groupingBy(ReservationProcess::getExternalPatientId));
        int changed = 0;
        for (Map.Entry<String, List<ReservationProcess>> patient : byPatient.entrySet()) {
            changed += reconcile(patient.getValue(), catedraReservationClient.findReservationsOfPatient(patient.getKey()));
        }
        if (changed > 0) {
            log.info("Reconciliación con la cátedra: {} procesos puestos al día", changed);
        }
        return changed;
    }

    private int reconcile(List<ReservationProcess> processes, List<CatedraReservation> catedraReservations) {
        Map<String, CatedraReservation> byProcess = catedraReservations.stream()
                .collect(Collectors.toMap(CatedraReservation::getCatedraProcessId, Function.identity(), (first, second) -> first));
        int changed = 0;
        for (ReservationProcess process : processes) {
            CatedraReservation catedra = byProcess.get(process.getCatedraProcessId());
            if (catedra != null && process.reconcileWith(catedra, clock.instant())) {
                reservationProcessRepository.saveReservationProcess(process);
                log.info("Proceso {} reconciliado: {}", process.getId(), process.getStatus());
                changed++;
            }
        }
        return changed;
    }
}
