package ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in;

import java.util.UUID;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;

public interface SubmitPhoneUseCase {

    /** El usuario responde el pedido de teléfono de su proceso (enunciado §7 paso 5). */
    ReservationProcess submitPhone(String login, UUID reservationId, String phoneNumber);
}
