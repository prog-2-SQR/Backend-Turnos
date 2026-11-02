package ar.edu.um.prog2.quiroga.turnos.reservation.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneNumberTest {

    // Primero se sacan espacios, guiones, paréntesis y puntos; después se valida (anexo §15.5).
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "+5492615551234      | +5492615551234",
            "+54 9 261 555-1234  | +5492615551234",
            "(261) 555.1234      | 2615551234",
            "4251234             | 4251234"})
    void aValidPhoneIsNormalized(String raw, String expected) {
        assertThat(PhoneNumber.normalize(raw)).contains(expected);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"123456", "1234567890123456", "+54 9 261 abc", "54+9261555", "++5492615551234"})
    void anInvalidPhoneIsRejected(String raw) {
        assertThat(PhoneNumber.normalize(raw)).isEmpty();
    }
}
