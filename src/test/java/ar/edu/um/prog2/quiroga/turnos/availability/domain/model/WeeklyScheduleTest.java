package ar.edu.um.prog2.quiroga.turnos.availability.domain.model;

import static ar.edu.um.prog2.quiroga.turnos.availability.support.AvailabilityFixtures.MONDAY;
import static ar.edu.um.prog2.quiroga.turnos.availability.support.AvailabilityFixtures.mondayNoonToThree;
import static ar.edu.um.prog2.quiroga.turnos.availability.support.AvailabilityFixtures.schedule;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class WeeklyScheduleTest {

    @Test
    void generatesTheSlotsOfTheDayFromStartToEnd() {
        List<AvailableSlot> slots = mondayNoonToThree().slotsOn(MONDAY);

        assertThat(slots).extracting(AvailableSlot::getStartTime).containsExactly(
                LocalTime.of(12, 0), LocalTime.of(12, 30), LocalTime.of(13, 0),
                LocalTime.of(13, 30), LocalTime.of(14, 0), LocalTime.of(14, 30));
        assertThat(slots.getLast().getEndTime()).isEqualTo(LocalTime.of(15, 0));
        assertThat(slots).allSatisfy(slot -> assertThat(slot.getDate()).isEqualTo(MONDAY));
    }

    @Test
    void aSlotThatDoesNotFitBeforeTheEndIsNotOffered() {
        // 09:00 a 10:40 con turnos de 30: el de 10:30 terminaría 11:00, después del cierre.
        List<AvailableSlot> slots = schedule(DayOfWeek.MONDAY, "09:00", "10:40", 30).slotsOn(MONDAY);

        assertThat(slots).extracting(AvailableSlot::getStartTime)
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0));
    }

    @Test
    void otherDaysGenerateNothing() {
        assertThat(mondayNoonToThree().slotsOn(MONDAY.plusDays(1))).isEmpty();
    }

    @Test
    void disabledScheduleGeneratesNothing() {
        WeeklySchedule disabled = mondayNoonToThree();
        disabled.setEnabled(false);

        assertThat(disabled.slotsOn(MONDAY)).isEmpty();
    }

    @Test
    void invalidDurationGeneratesNothing() {
        assertThat(schedule(DayOfWeek.MONDAY, "12:00", "15:00", 0).slotsOn(MONDAY)).isEmpty();
    }

    @Test
    void scheduleWithoutDurationGeneratesNothing() {
        WeeklySchedule withoutDuration = mondayNoonToThree();
        withoutDuration.setSlotDurationMinutes(null);

        assertThat(withoutDuration.slotsOn(MONDAY)).isEmpty();
    }

    @Test
    void lateScheduleDoesNotGoPastMidnight() {
        List<AvailableSlot> slots = schedule(DayOfWeek.MONDAY, "23:00", "23:59", 30).slotsOn(MONDAY);

        assertThat(slots).extracting(AvailableSlot::getStartTime).containsExactly(LocalTime.of(23, 0));
    }
}
