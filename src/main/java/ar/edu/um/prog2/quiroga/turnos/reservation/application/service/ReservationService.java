package ar.edu.um.prog2.quiroga.turnos.reservation.application.service;

import java.util.UUID;

import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationRequest;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in.GetReservationUseCase;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in.StartReservationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final StartReservationUseCase startReservationUseCase;
    private final GetReservationUseCase getReservationUseCase;

    public ReservationProcess startReservation(String login, ReservationRequest request) {
        return startReservationUseCase.startReservation(login, request);
    }

    public ReservationProcess getReservation(String login, UUID id) {
        return getReservationUseCase.getReservation(login, id)
                .orElseThrow(() -> new ReservationException(ReservationException.RESERVATION_NOT_FOUND, "No existe la reserva " + id));
    }
}
