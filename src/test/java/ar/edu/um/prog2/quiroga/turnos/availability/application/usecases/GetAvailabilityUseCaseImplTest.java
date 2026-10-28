package ar.edu.um.prog2.quiroga.turnos.availability.application.usecases;

import static ar.edu.um.prog2.quiroga.turnos.availability.support.AvailabilityFixtures.ANA_TORRES_ID;
import static ar.edu.um.prog2.quiroga.turnos.availability.support.AvailabilityFixtures.MONDAY;
import static ar.edu.um.prog2.quiroga.turnos.availability.support.AvailabilityFixtures.anaTorres;
import static ar.edu.um.prog2.quiroga.turnos.availability.support.AvailabilityFixtures.mondayNoonToThree;
import static ar.edu.um.prog2.quiroga.turnos.availability.support.AvailabilityFixtures.occupied;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import ar.edu.um.prog2.quiroga.turnos.availability.application.exception.AvailabilityException;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.Availability;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.AvailableSlot;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.Professional;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.ports.out.CatalogClient;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.ports.out.OccupancyClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetAvailabilityUseCaseImplTest {

    private static final ZoneId ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

    @Mock
    private CatalogClient catalogClient;

    @Mock
    private OccupancyClient occupancyClient;

    private GetAvailabilityUseCaseImpl useCaseAt(int dayOffset, int hour) {
        ZonedDateTime now = MONDAY.plusDays(dayOffset).atTime(hour, 0).atZone(ARGENTINA);
        return new GetAvailabilityUseCaseImpl(catalogClient, occupancyClient, Clock.fixed(now.toInstant(), ARGENTINA));
    }

    @Test
    void combinesTheSchedulesWithTheOccupanciesOfThatDay() {
        when(catalogClient.findProfessional(ANA_TORRES_ID)).thenReturn(Optional.of(anaTorres()));
        when(catalogClient.findWeeklySchedules(ANA_TORRES_ID)).thenReturn(List.of(mondayNoonToThree()));
        when(occupancyClient.findOccupancies(ANA_TORRES_ID, MONDAY, MONDAY)).thenReturn(List.of(occupied(MONDAY, "12:00")));

        Availability availability = useCaseAt(-1, 10).getAvailability(ANA_TORRES_ID, MONDAY);

        assertThat(availability.getSlots()).extracting(AvailableSlot::getStartTime).containsExactly(
                LocalTime.of(12, 30), LocalTime.of(13, 0), LocalTime.of(13, 30), LocalTime.of(14, 0), LocalTime.of(14, 30));
    }

    @Test
    void todayOnlyOffersTheSlotsThatHaveNotStarted() {
        when(catalogClient.findProfessional(ANA_TORRES_ID)).thenReturn(Optional.of(anaTorres()));
        when(catalogClient.findWeeklySchedules(ANA_TORRES_ID)).thenReturn(List.of(mondayNoonToThree()));
        when(occupancyClient.findOccupancies(ANA_TORRES_ID, MONDAY, MONDAY)).thenReturn(List.of());

        // Lunes 14:00 en Argentina: quedan 14:30 (el de 14:00 ya empezó).
        Availability availability = useCaseAt(0, 14).getAvailability(ANA_TORRES_ID, MONDAY);

        assertThat(availability.getSlots()).extracting(AvailableSlot::getStartTime).containsExactly(LocalTime.of(14, 30));
    }

    @Test
    void pastDateIsRejectedWithoutCallingAnyService() {
        assertThatThrownBy(() -> useCaseAt(1, 9).getAvailability(ANA_TORRES_ID, MONDAY))
                .isInstanceOf(AvailabilityException.class)
                .extracting("code").isEqualTo(AvailabilityException.INVALID_DATE);
        verifyNoInteractions(catalogClient, occupancyClient);
    }

    @Test
    void unknownProfessionalIsNotFound() {
        when(catalogClient.findProfessional(ANA_TORRES_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCaseAt(-1, 10).getAvailability(ANA_TORRES_ID, MONDAY))
                .isInstanceOf(AvailabilityException.class)
                .extracting("code").isEqualTo(AvailabilityException.PROFESSIONAL_NOT_FOUND);
        verifyNoInteractions(occupancyClient);
    }

    @Test
    void disabledProfessionalIsRejectedBeforeAskingTheCatedra() {
        Professional disabled = anaTorres();
        disabled.setEnabled(false);
        when(catalogClient.findProfessional(ANA_TORRES_ID)).thenReturn(Optional.of(disabled));

        assertThatThrownBy(() -> useCaseAt(-1, 10).getAvailability(ANA_TORRES_ID, MONDAY))
                .isInstanceOf(AvailabilityException.class)
                .extracting("code").isEqualTo(AvailabilityException.PROFESSIONAL_DISABLED);
        verify(catalogClient, never()).findWeeklySchedules(anyLong());
        verify(occupancyClient, never()).findOccupancies(anyLong(), any(), any());
    }
}
