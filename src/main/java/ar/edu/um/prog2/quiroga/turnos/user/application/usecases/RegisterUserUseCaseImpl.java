package ar.edu.um.prog2.quiroga.turnos.user.application.usecases;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;

import ar.edu.um.prog2.quiroga.turnos.user.application.exception.UserException;
import ar.edu.um.prog2.quiroga.turnos.user.domain.model.Authorities;
import ar.edu.um.prog2.quiroga.turnos.user.domain.model.User;
import ar.edu.um.prog2.quiroga.turnos.user.domain.ports.in.RegisterUserUseCase;
import ar.edu.um.prog2.quiroga.turnos.user.domain.ports.out.PasswordHasher;
import ar.edu.um.prog2.quiroga.turnos.user.domain.ports.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro con el comportamiento observable de JHipster: login y email en minúsculas, únicos,
 * usuario activo desde el inicio (enunciado §5) y rol ROLE_USER. Los datos internos los pone el backend.
 */
@Component
@RequiredArgsConstructor
public class RegisterUserUseCaseImpl implements RegisterUserUseCase {

    static final int PASSWORD_MIN_LENGTH = 4;
    static final int PASSWORD_MAX_LENGTH = 100;
    static final String DEFAULT_LANG_KEY = "es";
    static final String SYSTEM = "system";

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    @Override
    @Transactional
    public User registerUser(User user, String password) {
        checkPassword(password);
        String login = user.getLogin().toLowerCase(Locale.ROOT);
        String email = lowerCaseOrNull(user.getEmail());
        checkAvailable(login, email);
        Instant now = Instant.now();
        User newUser = User.builder()
                .login(login)
                .passwordHash(passwordHasher.hash(password))
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(email)
                .imageUrl(user.getImageUrl())
                .activated(true)
                .langKey(user.getLangKey() == null ? DEFAULT_LANG_KEY : user.getLangKey())
                .authorities(Set.of(Authorities.USER))
                .createdBy(SYSTEM)
                .createdDate(now)
                .lastModifiedBy(SYSTEM)
                .lastModifiedDate(now)
                .build();
        return userRepository.saveUser(newUser);
    }

    private static void checkPassword(String password) {
        if (password == null || password.length() < PASSWORD_MIN_LENGTH || password.length() > PASSWORD_MAX_LENGTH) {
            throw new UserException(UserException.INVALID_PASSWORD, "La contraseña debe tener entre 4 y 100 caracteres");
        }
    }

    private void checkAvailable(String login, String email) {
        if (userRepository.existsUserByLogin(login)) {
            throw new UserException(UserException.USERNAME_ALREADY_EXISTS, "El login ya está en uso");
        }
        if (email != null && userRepository.existsUserByEmail(email)) {
            throw new UserException(UserException.EMAIL_ALREADY_EXISTS, "El email ya está en uso");
        }
    }

    private static String lowerCaseOrNull(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }
}
