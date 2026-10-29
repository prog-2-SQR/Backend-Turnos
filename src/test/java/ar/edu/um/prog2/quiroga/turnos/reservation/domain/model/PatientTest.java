package ar.edu.um.prog2.quiroga.turnos.reservation.domain.model;

import static ar.edu.um.prog2.quiroga.turnos.reservation.support.ReservationFixtures.juan;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PatientTest {

    @Test
    void theExternalIdDoesNotExposeTheLogin() {
        assertThat(juan().externalId()).isEqualTo("user-42");
    }

    @Test
    void namesOfTheRequestWinOverTheAccountOnes() {
        Patient patient = juan().withNames(" Juan Carlos ", "Pérez");

        assertThat(patient.getFirstName()).isEqualTo("Juan Carlos");
        assertThat(patient.getLastName()).isEqualTo("Pérez");
    }

    @Test
    void withoutNamesInTheRequestTheAccountOnesAreKept() {
        Patient patient = juan().withNames(null, " ");

        assertThat(patient.getFirstName()).isEqualTo("Juan");
        assertThat(patient.getLastName()).isEqualTo("Perez");
    }

    @Test
    void theCatedraNeedsBothNamesWithTwoToOneHundredCharacters() {
        assertThat(juan().hasValidName()).isTrue();
        assertThat(juan().toBuilder().lastName(null).build().hasValidName()).isFalse();
        assertThat(juan().toBuilder().firstName("J").build().hasValidName()).isFalse();
        assertThat(juan().toBuilder().firstName("J".repeat(101)).build().hasValidName()).isFalse();
    }
}
