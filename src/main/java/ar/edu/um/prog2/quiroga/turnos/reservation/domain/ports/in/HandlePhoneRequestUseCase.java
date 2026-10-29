package ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.PhoneRequest;

public interface HandlePhoneRequestUseCase {

    /** Registra en el proceso que la cátedra pidió el teléfono. Un pedido repetido (mismo eventId) no tiene efecto. */
    void handlePhoneRequest(PhoneRequest request);
}
