package ar.edu.um.prog2.quiroga.turnos.user.domain.ports.out;

import ar.edu.um.prog2.quiroga.turnos.user.domain.model.User;

public interface UserRepository {

    boolean existsUserByLogin(String login);

    boolean existsUserByEmail(String email);

    User saveUser(User user);
}
