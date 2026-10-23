package ar.edu.um.prog2.quiroga.turnos.user.domain.ports.out;

import java.util.Optional;

import ar.edu.um.prog2.quiroga.turnos.user.domain.model.User;

public interface UserRepository {

    Optional<User> findUserByLogin(String login);

    boolean existsUserByLogin(String login);

    boolean existsUserByEmail(String email);

    User saveUser(User user);
}
