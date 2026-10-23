package ar.edu.um.prog2.quiroga.turnos.user.application.service;

import ar.edu.um.prog2.quiroga.turnos.user.domain.model.User;
import ar.edu.um.prog2.quiroga.turnos.user.domain.ports.in.RegisterUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final RegisterUserUseCase registerUserUseCase;

    public User registerUser(User user, String password) {
        return registerUserUseCase.registerUser(user, password);
    }
}
