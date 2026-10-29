package ar.edu.um.prog2.quiroga.turnos.reservation.infrastructure.availability;

import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.MONDAY;
import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.TWELVE_THIRTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import ar.edu.um.prog2.quiroga.turnos.availability.application.exception.AvailabilityException;
import ar.edu.um.prog2.quiroga.turnos.availability.application.service.AvailabilityService;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.Availability;
import ar.edu.um.prog2.quiroga.turnos.availability.domain.model.AvailableSlot;
import ar.edu.um.prog2.quiroga.turnos.reservation.application.exception.ReservationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AvailabilitySlotAdapterTest {

    @Mock
    private AvailabilityService availabilityService;

    @InjectMocks
    private AvailabilitySlotAdapter adapter;

    private void freeSlotsAt(LocalTime... starts) {
        List<AvailableSlot> slots = Arrays.stream(starts)
                .map(start -> AvailableSlot.builder().date(MONDAY).startTime(start).endTime(start.plusMinutes(30)).build())
                .toList();
        when(availabilityService.getAvailability(201L, MONDAY))
                .thenReturn(Availability.builder().professionalId(201L).date(MONDAY).slots(slots).build());
    }

    @Test
    void aFreeSlotIsAvailable() {
        freeSlotsAt(LocalTime.of(12, 0), TWELVE_THIRTY);

        assertThat(adapter.isAvailable(201L, MONDAY, TWELVE_THIRTY)).isTrue();
    }

    @Test
    void aSlotThatIsNotAmongTheFreeOnesIsNotAvailable() {
        freeSlotsAt(LocalTime.of(12, 0));

        assertThat(adapter.isAvailable(201L, MONDAY, TWELVE_THIRTY)).isFalse();
    }

    @Test
    void errorsOfTheAvailabilityKeepTheirCode() {
        when(availabilityService.getAvailability(201L, MONDAY))
                .thenThrow(new AvailabilityException(AvailabilityException.CATALOG_UNAVAILABLE, "El catálogo no está disponible"));

        assertThatThrownBy(() -> adapter.isAvailable(201L, MONDAY, TWELVE_THIRTY))
                .isInstanceOf(ReservationException.class)
                .extracting("code").isEqualTo(ReservationException.CATALOG_UNAVAILABLE);
    }
}
