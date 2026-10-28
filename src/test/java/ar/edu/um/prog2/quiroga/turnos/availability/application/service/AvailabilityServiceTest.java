package ar.edu.um.prog2.quiroga.turnos.availability.application.service;

import static ar.edu.um.prog2.quiroga.turnos.availability.support.AvailabilityFixtures.ANA_TORRES_ID;
import static ar.edu.um.prog2.quiroga.turnos.availability.support.AvailabilityFixtures.MONDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.Availability;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.ports.in.GetAvailabilityUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private GetAvailabilityUseCase getAvailabilityUseCase;

    @InjectMocks
    private AvailabilityService availabilityService;

    @Test
    void getAvailabilityDelegatesToItsUseCase() {
        Availability availability = Availability.builder().professionalId(ANA_TORRES_ID).date(MONDAY).slots(List.of()).build();
        when(getAvailabilityUseCase.getAvailability(ANA_TORRES_ID, MONDAY)).thenReturn(availability);

        assertThat(availabilityService.getAvailability(ANA_TORRES_ID, MONDAY)).isSameAs(availability);
    }
}
