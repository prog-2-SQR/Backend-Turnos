package ar.edu.um.prog2.quiroga.turnos.user.application.service;

import static ar.edu.um.prog2.quiroga.turnos.user.support.UserFixtures.juan;
import static ar.edu.um.prog2.quiroga.turnos.user.support.UserFixtures.registration;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import ar.edu.um.prog2.quiroga.turnos.user.domain.model.User;
import ar.edu.um.prog2.quiroga.turnos.user.domain.ports.in.RegisterUserUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private RegisterUserUseCase registerUserUseCase;

    @InjectMocks
    private UserService userService;

    @Test
    void registerUserDelegatesToTheUseCase() {
        User registered = juan();
        when(registerUserUseCase.registerUser(any(User.class), eq("clave"))).thenReturn(registered);

        assertThat(userService.registerUser(registration(), "clave")).isSameAs(registered);
    }
}
