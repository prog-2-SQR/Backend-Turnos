package ar.edu.um.prog2.quiroga.turnos.availability.domain.model;

import static ar.edu.um.prog2.quiroga.turnos.availability.support.AvailabilityFixtures.ANA_TORRES_ID;
import static ar.edu.um.prog2.quiroga.turnos.availability.support.AvailabilityFixtures.MONDAY;
import static ar.edu.um.prog2.quiroga.turnos.availability.support.AvailabilityFixtures.mondayNoonToThree;
import static ar.edu.um.prog2.quiroga.turnos.availability.support.AvailabilityFixtures.occupied;
import static ar.edu.um.prog2.quiroga.turnos.availability.support.AvailabilityFixtures.schedule;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class AvailabilityTest {

    private static final LocalDateTime THE_DAY_BEFORE = MONDAY.minusDays(1).atTime(10, 0);

    @Test
    void occupiedSlotsAreNotOffered() {
        Availability availability = Availability.calculate(ANA_TORRES_ID, MONDAY, List.of(mondayNoonToThree()),
                List.of(occupied(MONDAY, "12:30"), occupied(MONDAY, "14:00")), THE_DAY_BEFORE);

        assertThat(availability.getProfessionalId()).isEqualTo(ANA_TORRES_ID);
        assertThat(availability.getDate()).isEqualTo(MONDAY);
        assertThat(availability.getSlots()).extracting(AvailableSlot::getStartTime).containsExactly(
                LocalTime.of(12, 0), LocalTime.of(13, 0), LocalTime.of(13, 30), LocalTime.of(14, 30));
    }

    @Test
    void occupanciesOfOtherDatesDoNotMatter() {
        Availability availability = Availability.calculate(ANA_TORRES_ID, MONDAY, List.of(mondayNoonToThree()),
                List.of(occupied(MONDAY.plusWeeks(1), "12:00")), THE_DAY_BEFORE);

        assertThat(availability.getSlots()).hasSize(6);
    }

    @Test
    void slotsThatAlreadyStartedAreNotOffered() {
        LocalDateTime mondayAtOne = MONDAY.atTime(13, 0);

        Availability availability = Availability.calculate(ANA_TORRES_ID, MONDAY, List.of(mondayNoonToThree()),
                List.of(), mondayAtOne);

        assertThat(availability.getSlots()).extracting(AvailableSlot::getStartTime)
                .containsExactly(LocalTime.of(13, 30), LocalTime.of(14, 0), LocalTime.of(14, 30));
    }

    @Test
    void slotsOfSeveralSchedulesComeOrderedByTime() {
        Availability availability = Availability.calculate(ANA_TORRES_ID, MONDAY,
                List.of(schedule(DayOfWeek.MONDAY, "16:00", "17:00", 30), schedule(DayOfWeek.MONDAY, "09:00", "10:00", 30)),
                List.of(), THE_DAY_BEFORE);

        assertThat(availability.getSlots()).extracting(AvailableSlot::getStartTime).containsExactly(
                LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(16, 0), LocalTime.of(16, 30));
    }

    @Test
    void dayWithoutSchedulesHasNoSlots() {
        Availability availability = Availability.calculate(ANA_TORRES_ID, MONDAY.plusDays(1), List.of(mondayNoonToThree()),
                List.of(), THE_DAY_BEFORE);

        assertThat(availability.getSlots()).isEmpty();
    }
}
