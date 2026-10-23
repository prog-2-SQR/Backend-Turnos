package ar.edu.um.prog2.quiroga.turnos.user.application.usecases;

import static ar.edu.um.prog2.quiroga.turnos.user.support.UserFixtures.registration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.um.prog2.quiroga.turnos.user.application.exception.UserException;
import ar.edu.um.prog2.quiroga.turnos.user.domain.model.Authorities;
import ar.edu.um.prog2.quiroga.turnos.user.domain.model.User;
import ar.edu.um.prog2.quiroga.turnos.user.domain.ports.out.PasswordHasher;
import ar.edu.um.prog2.quiroga.turnos.user.domain.ports.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseImplTest {

    private static final String PASSWORD = "una-clave-segura";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @InjectMocks
    private RegisterUserUseCaseImpl registerUserUseCase;

    @Test
    void registersAnActiveUserWithRoleUserAndHashedPassword() {
        when(passwordHasher.hash(PASSWORD)).thenReturn("hash");
        when(userRepository.saveUser(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        registerUserUseCase.registerUser(registration(), PASSWORD);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveUser(saved.capture());
        User user = saved.getValue();
        assertThat(user.getLogin()).isEqualTo("juan.perez");
        assertThat(user.getEmail()).isEqualTo("juan.perez@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("hash");
        assertThat(user.getActivated()).isTrue();
        assertThat(user.getAuthorities()).containsExactly(Authorities.USER);
        assertThat(user.getLangKey()).isEqualTo("es");
        assertThat(user.getFirstName()).isEqualTo("Juan");
        assertThat(user.getImageUrl()).isEqualTo("https://example.com/juan.png");
        assertThat(user.getCreatedBy()).isEqualTo("system");
        assertThat(user.getCreatedDate()).isNotNull().isEqualTo(user.getLastModifiedDate());
    }

    @Test
    void keepsTheLanguageChosenByTheUser() {
        User registration = registration();
        registration.setLangKey("en");
        when(userRepository.saveUser(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = registerUserUseCase.registerUser(registration, PASSWORD);

        assertThat(user.getLangKey()).isEqualTo("en");
    }

    @Test
    void emailIsOptional() {
        User registration = registration();
        registration.setEmail(null);
        when(userRepository.saveUser(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = registerUserUseCase.registerUser(registration, PASSWORD);

        assertThat(user.getEmail()).isNull();
        verify(userRepository, never()).existsUserByEmail(anyString());
    }

    @Test
    void rejectsALoginAlreadyInUseIgnoringCase() {
        when(userRepository.existsUserByLogin("juan.perez")).thenReturn(true);

        assertThatThrownBy(() -> registerUserUseCase.registerUser(registration(), PASSWORD))
                .isInstanceOf(UserException.class)
                .extracting("code").isEqualTo(UserException.USERNAME_ALREADY_EXISTS);
        verify(userRepository, never()).saveUser(any());
    }

    @Test
    void rejectsAnEmailAlreadyInUseIgnoringCase() {
        when(userRepository.existsUserByEmail("juan.perez@example.com")).thenReturn(true);

        assertThatThrownBy(() -> registerUserUseCase.registerUser(registration(), PASSWORD))
                .isInstanceOf(UserException.class)
                .extracting("code").isEqualTo(UserException.EMAIL_ALREADY_EXISTS);
        verify(userRepository, never()).saveUser(any());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"abc", "una-clave-de-mas-de-cien-caracteres-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"})
    void rejectsPasswordsOutsideFourToOneHundredCharacters(String password) {
        assertThatThrownBy(() -> registerUserUseCase.registerUser(registration(), password))
                .isInstanceOf(UserException.class)
                .extracting("code").isEqualTo(UserException.INVALID_PASSWORD);
        verify(userRepository, never()).saveUser(any());
    }
}
