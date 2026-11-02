package ar.edu.um.prog2.quiroga.turnos.reservation.application.usecases;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationPage;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in.ListReservationsUseCase;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.PatientDirectory;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// Aislamiento entre usuarios (enunciado §4.2): el usuario sale del token y la consulta filtra por su id.
@Component
@RequiredArgsConstructor
public class ListReservationsUseCaseImpl implements ListReservationsUseCase {

    private final PatientDirectory patientDirectory;
    private final ReservationProcessRepository reservationProcessRepository;

    @Override
    public ReservationPage listReservations(String login, int page, int size) {
        Long userId = patientDirectory.findPatient(login).getUserId();
        return reservationProcessRepository.findReservationProcessesByUser(userId, page, size);
    }
}
