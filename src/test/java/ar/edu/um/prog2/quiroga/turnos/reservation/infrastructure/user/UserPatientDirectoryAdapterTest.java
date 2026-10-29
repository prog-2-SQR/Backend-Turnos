package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import ar.edu.um.prog2.quiroga.turnos.reservation.domain.model.Patient;
import ar.edu.um.prog2.quiroga.turnos.user.application.service.UserService;
import ar.edu.um.prog2.quiroga.turnos.user.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserPatientDirectoryAdapterTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserPatientDirectoryAdapter adapter;

    @Test
    void thePatientIsTheUserWithItsIdAndNames() {
        when(userService.findUserByLogin("juan.perez"))
                .thenReturn(User.builder().id(42L).login("juan.perez").firstName("Juan").lastName("Perez").build());

        Patient patient = adapter.findPatient("juan.perez");

        assertThat(patient.getUserId()).isEqualTo(42L);
        assertThat(patient.getFirstName()).isEqualTo("Juan");
        assertThat(patient.getLastName()).isEqualTo("Perez");
        assertThat(patient.externalId()).isEqualTo("user-42");
    }
}
