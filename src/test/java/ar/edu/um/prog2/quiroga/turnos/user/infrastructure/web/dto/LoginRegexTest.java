package ar.edu.um.prog2.quiroga.turnos.user.infrastructure.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * El regex del login, comparado contra el texto entero como hace @Pattern. Los casos fijan el comportamiento del
 * regex de JHipster, para que el arreglo de Sonar (agrupar las alternativas, repetición posesiva) no cambie nada.
 */
class LoginRegexTest {

    private static final Pattern LOGIN = Pattern.compile(RegisterUserRequest.LOGIN_REGEX);

    // Emails, logins con _.@- y "a@b_c", que no es email pero entra por la segunda alternativa.
    @ParameterizedTest
    @ValueSource(strings = {"juan.perez", "juan_perez-2", "juan@example.com", "juan+turnos@mail.example.com", "a@b_c", "@@@"})
    void acceptsTheLoginsOfJhipster(String login) {
        assertThat(LOGIN.matcher(login).matches()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"juan perez", "juan!", "juan#perez", "juán", ""})
    void rejectsSpacesAndOtherSymbols(String login) {
        assertThat(LOGIN.matcher(login).matches()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"juan.perez", "juan@example.com", "a@b_c", "juan perez", "juan!", "juán"})
    void behavesLikeTheOriginalRegex(String login) {
        Pattern original = Pattern.compile("^(?>[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*)|(?>[_.@A-Za-z0-9-]+)$");

        assertThat(LOGIN.matcher(login).matches()).isEqualTo(original.matcher(login).matches());
    }
}
