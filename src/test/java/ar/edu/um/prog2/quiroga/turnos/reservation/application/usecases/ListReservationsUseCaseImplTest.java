package ar.edu.um.prog2.quiroga.turnos.reservation.application.usecases;

import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.juan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.ReservationPage;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.PatientDirectory;
import ar.edu.um.prog2.quiroga.turnos.reservation.domain.ports.out.ReservationProcessRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListReservationsUseCaseImplTest {

    @Mock
    private PatientDirectory patientDirectory;

    @Mock
    private ReservationProcessRepository reservationProcessRepository;

    @InjectMocks
    private ListReservationsUseCaseImpl useCase;

    @Test
    void listsTheProcessesOfTheUserOfTheLogin() {
        ReservationPage page = ReservationPage.builder().page(1).size(10).totalElements(12).build();
        when(patientDirectory.findPatient("juan.perez")).thenReturn(juan());
        when(reservationProcessRepository.findReservationProcessesByUser(42L, 1, 10)).thenReturn(page);

        assertThat(useCase.listReservations("juan.perez", 1, 10)).isSameAs(page);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    @Test
    void anEmptyPageHasNoPages() {
        assertThat(ReservationPage.builder().build().totalPages()).isZero();
    }
}
