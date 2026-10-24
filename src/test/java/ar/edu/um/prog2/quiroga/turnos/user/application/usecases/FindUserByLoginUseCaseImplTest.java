package ar.edu.um.prog2.quiroga.turnos.user.application.usecases;

import static ar.edu.um.prog2.quiroga.turnos.user.support.UserFixtures.juan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import ar.edu.um.prog2.quiroga.turnos.user.domain.ports.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FindUserByLoginUseCaseImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FindUserByLoginUseCaseImpl findUserByLoginUseCase;

    @Test
    void searchesTheLoginInLowerCase() {
        when(userRepository.findUserByLogin("juan.perez")).thenReturn(Optional.of(juan()));

        assertThat(findUserByLoginUseCase.findUserByLogin("Juan.Perez")).isPresent();
    }
}
