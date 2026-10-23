package ar.edu.um.prog2.quiroga.turnos.user.infrastructure.security;

import static ar.edu.um.prog2.quiroga.turnos.user.support.UserFixtures.HASH;
import static ar.edu.um.prog2.quiroga.turnos.user.support.UserFixtures.juan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import ar.edu.um.prog2.quiroga.turnos.user.application.exception.UserException;
import ar.edu.um.prog2.quiroga.turnos.user.application.service.UserService;
import ar.edu.um.prog2.quiroga.turnos.user.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceAdapterTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserDetailsServiceAdapter adapter;

    @Test
    void returnsTheLoginHashAndRolesOfTheUser() {
        when(userService.findUserByLogin("juan.perez")).thenReturn(juan());

        UserDetails details = adapter.loadUserByUsername("juan.perez");

        assertThat(details.getUsername()).isEqualTo("juan.perez");
        assertThat(details.getPassword()).isEqualTo(HASH);
        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_USER");
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void aNotActivatedUserIsDisabled() {
        User user = juan();
        user.setActivated(false);
        when(userService.findUserByLogin("juan.perez")).thenReturn(user);

        assertThat(adapter.loadUserByUsername("juan.perez").isEnabled()).isFalse();
    }

    @Test
    void unknownUserThrowsUsernameNotFound() {
        when(userService.findUserByLogin("nadie"))
                .thenThrow(new UserException(UserException.USER_NOT_FOUND, "No existe el usuario nadie"));

        assertThatThrownBy(() -> adapter.loadUserByUsername("nadie"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
