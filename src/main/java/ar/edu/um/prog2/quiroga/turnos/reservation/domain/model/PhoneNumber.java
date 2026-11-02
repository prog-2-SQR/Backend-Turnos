package ar.edu.um.prog2.quiroga.turnos.reservation.domain.model;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Regla de la cátedra para el teléfono (anexo §15.5): se sacan espacios, guiones, paréntesis y puntos, y lo que queda
 * tiene que cumplir ^\+?[0-9]{7,15}$. Se valida igual acá para avisarle al usuario en el momento, sin esperar el
 * rechazo por Kafka.
 */
public final class PhoneNumber {

    private static final Pattern SEPARATORS = Pattern.compile("[\\s\\-().]");
    private static final Pattern VALID = Pattern.compile("^\\+?\\d{7,15}$");

    private PhoneNumber() {
    }

    /** El teléfono normalizado, o vacío si no cumple la regla. */
    public static Optional<String> normalize(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String normalized = SEPARATORS.matcher(raw).replaceAll("");
        return VALID.matcher(normalized).matches() ? Optional.of(normalized) : Optional.empty();
    }
}
