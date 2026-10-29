package ar.edu.um.prog2.quiroga.turnos.reservation.application.usecases;

import java.util.Optional;
import java.util.UUID;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in.GetReservationUseCase;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.PatientDirectory;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Aislamiento entre usuarios (enunciado §4.2): un proceso ajeno se trata igual que uno inexistente.
@Component
@RequiredArgsConstructor
public class GetReservationUseCaseImpl implements GetReservationUseCase {

    private final PatientDirectory patientDirectory;
    private final ReservationProcessRepository reservationProcessRepository;

    @Override
    public Optional<ReservationProcess> getReservation(String login, UUID id) {
        Long userId = patientDirectory.findPatient(login).getUserId();
        return reservationProcessRepository.findReservationProcessById(id)
                .filter(process -> process.belongsTo(userId));
    }
}
