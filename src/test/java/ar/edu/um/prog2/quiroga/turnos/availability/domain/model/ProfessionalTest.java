package ar.edu.um.prog2.quiroga.turnos.availability.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProfessionalTest {

    @Test
    void onlyAnEnabledProfessionalAcceptsAppointments() {
        assertThat(Professional.builder().enabled(true).build().isAcceptingAppointments()).isTrue();
        assertThat(Professional.builder().enabled(false).build().isAcceptingAppointments()).isFalse();
        assertThat(Professional.builder().build().isAcceptingAppointments()).isFalse();
    }
}
