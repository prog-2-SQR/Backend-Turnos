package ar.edu.um.prog2.quiroga.turnos.reservation.application.service;

import java.util.UUID;

import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.PhoneRequest;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationPage;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationRequest;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationResult;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in.CancelReservationUseCase;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in.GetReservationUseCase;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in.HandlePhoneRequestUseCase;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in.HandleReservationResultUseCase;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in.ListReservationsUseCase;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in.PublishPhoneSubmissionsUseCase;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in.StartReservationUseCase;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in.SubmitPhoneUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final StartReservationUseCase startReservationUseCase;
    private final GetReservationUseCase getReservationUseCase;
    private final HandlePhoneRequestUseCase handlePhoneRequestUseCase;
    private final SubmitPhoneUseCase submitPhoneUseCase;
    private final PublishPhoneSubmissionsUseCase publishPhoneSubmissionsUseCase;
    private final HandleReservationResultUseCase handleReservationResultUseCase;
    private final ListReservationsUseCase listReservationsUseCase;
    private final CancelReservationUseCase cancelReservationUseCase;

    public ReservationProcess startReservation(String login, ReservationRequest request) {
        return startReservationUseCase.startReservation(login, request);
    }

    public ReservationProcess getReservation(String login, UUID id) {
        return getReservationUseCase.getReservation(login, id)
                .orElseThrow(() -> new ReservationException(ReservationException.RESERVATION_NOT_FOUND, "No existe la reserva " + id));
    }

    public ReservationProcess cancelReservation(String login, UUID id, String reason) {
        return cancelReservationUseCase.cancelReservation(login, id, reason);
    }

    public ReservationPage listReservations(String login, int page, int size) {
        return listReservationsUseCase.listReservations(login, page, size);
    }

    public void handlePhoneRequest(PhoneRequest request) {
        handlePhoneRequestUseCase.handlePhoneRequest(request);
    }

    public ReservationProcess submitPhone(String login, UUID id, String phoneNumber) {
        return submitPhoneUseCase.submitPhone(login, id, phoneNumber);
    }

    public void publishPendingPhoneSubmissions(int maxAttempts) {
        publishPhoneSubmissionsUseCase.publishPendingPhoneSubmissions(maxAttempts);
    }

    public void handleReservationResult(ReservationResult result) {
        handleReservationResultUseCase.handleReservationResult(result);
    }
}
