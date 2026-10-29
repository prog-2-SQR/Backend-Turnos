package ar.edu.um.prog2.quiroga.turnos.reservation.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** El usuario que reserva, visto como paciente de la cátedra. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Patient {

    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 100;

    private Long userId;
    private String firstName;
    private String lastName;

    /** Identificador opaco para la cátedra (anexo §9): no expone el login del usuario. */
    public String externalId() {
        return "user-" + userId;
    }

    /** Usa los nombres indicados en el pedido cuando vienen; si no, los de la cuenta. */
    public Patient withNames(String requestedFirstName, String requestedLastName) {
        return toBuilder()
                .firstName(isBlank(requestedFirstName) ? firstName : requestedFirstName.trim())
                .lastName(isBlank(requestedLastName) ? lastName : requestedLastName.trim())
                .build();
    }

    /** La confirmación del hold exige nombre y apellido de 2 a 100 caracteres (anexo §10). */
    public boolean hasValidName() {
        return isValidName(firstName) && isValidName(lastName);
    }

    private static boolean isValidName(String name) {
        return !isBlank(name) && name.trim().length() >= MIN_NAME_LENGTH && name.trim().length() <= MAX_NAME_LENGTH;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
