package ar.edu.um.prog2.quiroga.turnos.user.domain.ports.in;

import ar.edu.um.prog2.quiroga.turnos.user.domain.model.User;

public interface RegisterUserUseCase {

    /** Registra un usuario nuevo con la contraseña en claro, que solo se usa para calcular el hash. */
    User registerUser(User user, String password);
}
