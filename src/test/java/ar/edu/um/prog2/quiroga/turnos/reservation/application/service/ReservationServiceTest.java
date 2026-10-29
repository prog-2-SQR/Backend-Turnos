package ar.edu.um.prog2.quiroga.turnos.reservation.application.service;

import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.mondayAtTwelveThirty;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.phoneRequest;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.requested;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.PhoneRequest;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationRequest;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in.GetReservationUseCase;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in.HandlePhoneRequestUseCase;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in.StartReservationUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private StartReservationUseCase startReservationUseCase;

    @Mock
    private GetReservationUseCase getReservationUseCase;

    @Mock
    private HandlePhoneRequestUseCase handlePhoneRequestUseCase;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    void startReservationDelegatesToItsUseCase() {
        ReservationRequest request = mondayAtTwelveThirty();
        ReservationProcess process = requested();
        when(startReservationUseCase.startReservation("juan.perez", request)).thenReturn(process);

        assertThat(reservationService.startReservation("juan.perez", request)).isSameAs(process);
    }

    @Test
    void getReservationReturnsTheProcess() {
        ReservationProcess process = requested();
        when(getReservationUseCase.getReservation("juan.perez", process.getId())).thenReturn(Optional.of(process));

        assertThat(reservationService.getReservation("juan.perez", process.getId())).isSameAs(process);
    }

    @Test
    void aMissingReservationIsNotFound() {
        UUID id = UUID.randomUUID();
        when(getReservationUseCase.getReservation("juan.perez", id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.getReservation("juan.perez", id))
                .isInstanceOf(ReservationException.class)
                .extracting("code").isEqualTo(ReservationException.RESERVATION_NOT_FOUND);
    }

    @Test
    void handlePhoneRequestDelegatesToItsUseCase() {
        PhoneRequest request = phoneRequest();

        reservationService.handlePhoneRequest(request);

        verify(handlePhoneRequestUseCase).handlePhoneRequest(request);
    }
}
