package ar.edu.um.prog2.quiroga.turnos.reservation.application.service;

import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.mondayAtTwelveThirty;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.phoneRequest;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.requested;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.result;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.PhoneRequest;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationPage;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationRequest;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationResult;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationResultType;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in.CancelReservationUseCase;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in.GetReservationUseCase;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in.HandlePhoneRequestUseCase;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in.HandleReservationResultUseCase;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in.ListReservationsUseCase;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in.PublishPhoneSubmissionsUseCase;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in.StartReservationUseCase;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.in.SubmitPhoneUseCase;
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

    @Mock
    private SubmitPhoneUseCase submitPhoneUseCase;

    @Mock
    private PublishPhoneSubmissionsUseCase publishPhoneSubmissionsUseCase;

    @Mock
    private HandleReservationResultUseCase handleReservationResultUseCase;

    @Mock
    private ListReservationsUseCase listReservationsUseCase;

    @Mock
    private CancelReservationUseCase cancelReservationUseCase;

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

    @Test
    void submitPhoneDelegatesToItsUseCase() {
        ReservationProcess process = requested();
        when(submitPhoneUseCase.submitPhone("juan.perez", process.getId(), "+5492615551234")).thenReturn(process);

        assertThat(reservationService.submitPhone("juan.perez", process.getId(), "+5492615551234")).isSameAs(process);
    }

    @Test
    void publishPendingPhoneSubmissionsDelegatesToItsUseCase() {
        reservationService.publishPendingPhoneSubmissions(10);

        verify(publishPhoneSubmissionsUseCase).publishPendingPhoneSubmissions(10);
    }

    @Test
    void handleReservationResultDelegatesToItsUseCase() {
        ReservationResult result = result(ReservationResultType.CONFIRMED);

        reservationService.handleReservationResult(result);

        verify(handleReservationResultUseCase).handleReservationResult(result);
    }

    @Test
    void listReservationsDelegatesToItsUseCase() {
        ReservationPage page = ReservationPage.builder().page(0).size(20).build();
        when(listReservationsUseCase.listReservations("juan.perez", 0, 20)).thenReturn(page);

        assertThat(reservationService.listReservations("juan.perez", 0, 20)).isSameAs(page);
    }

    @Test
    void cancelReservationDelegatesToItsUseCase() {
        ReservationProcess process = requested();
        when(cancelReservationUseCase.cancelReservation("juan.perez", process.getId(), "motivo")).thenReturn(process);

        assertThat(reservationService.cancelReservation("juan.perez", process.getId(), "motivo")).isSameAs(process);
    }
}
