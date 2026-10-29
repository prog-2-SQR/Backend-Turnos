package ar.edu.um.prog2.quiroga.turnos.reservation.application.usecases;

import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.juan;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.requested;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationProcess;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.PatientDirectory;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetReservationUseCaseImplTest {

    @Mock
    private PatientDirectory patientDirectory;

    @Mock
    private ReservationProcessRepository reservationProcessRepository;

    @InjectMocks
    private GetReservationUseCaseImpl useCase;

    @Test
    void theOwnerSeesItsProcess() {
        ReservationProcess process = requested();
        when(patientDirectory.findPatient("juan.perez")).thenReturn(juan());
        when(reservationProcessRepository.findReservationProcessById(process.getId())).thenReturn(Optional.of(process));

        assertThat(useCase.getReservation("juan.perez", process.getId())).contains(process);
    }

    @Test
    void anotherUserDoesNotSeeIt() {
        ReservationProcess process = requested();
        when(patientDirectory.findPatient("ana")).thenReturn(juan().toBuilder().userId(7L).build());
        when(reservationProcessRepository.findReservationProcessById(process.getId())).thenReturn(Optional.of(process));

        assertThat(useCase.getReservation("ana", process.getId())).isEmpty();
    }

    @Test
    void anUnknownProcessIsEmpty() {
        UUID id = UUID.randomUUID();
        when(patientDirectory.findPatient("juan.perez")).thenReturn(juan());
        when(reservationProcessRepository.findReservationProcessById(id)).thenReturn(Optional.empty());

        assertThat(useCase.getReservation("juan.perez", id)).isEmpty();
    }
}
