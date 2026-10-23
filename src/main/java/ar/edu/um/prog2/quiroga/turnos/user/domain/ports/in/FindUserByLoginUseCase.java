package ar.edu.um.prog2.quiroga.turnos.user.domain.ports.in;

import java.util.Optional;

import ar.edu.um.prog2.quiroga.turnos.user.domain.model.User;

public interface FindUserByLoginUseCase {

    Optional<User> findUserByLogin(String login);
}
