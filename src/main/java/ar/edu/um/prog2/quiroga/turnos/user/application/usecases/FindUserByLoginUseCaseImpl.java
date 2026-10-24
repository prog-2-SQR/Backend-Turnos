package ar.edu.um.prog2.quiroga.turnos.user.application.usecases;

import java.util.Locale;
import java.util.Optional;

import ar.edu.um.prog2.quiroga.turnos.user.domain.model.User;
import ar.edu.um.prog2.quiroga.turnos.user.domain.ports.in.FindUserByLoginUseCase;
import ar.edu.um.prog2.quiroga.turnos.user.domain.ports.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** El login se guarda en minúsculas, así que se busca en minúsculas: "Juan.Perez" encuentra a "juan.perez". */
@Component
@RequiredArgsConstructor
public class FindUserByLoginUseCaseImpl implements FindUserByLoginUseCase {

    private final UserRepository userRepository;

    @Override
    public Optional<User> findUserByLogin(String login) {
        return userRepository.findUserByLogin(login.toLowerCase(Locale.ROOT));
    }
}
