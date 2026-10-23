package ar.edu.um.prog2.quiroga.turnos.user.infrastructure.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.um.prog2.quiroga.turnos.user.domain.model.User;
import ar.edu.um.prog2.quiroga.turnos.user.infrastructure.web.dto.RegisterUserRequest;
import org.junit.jupiter.api.Test;

class UserDtoMapperTest {

    private final UserDtoMapper userDtoMapper = new UserDtoMapper();

    @Test
    void toDomainCopiesOnlyTheFieldsTheUserCanChoose() {
        RegisterUserRequest request = RegisterUserRequest.builder()
                .login("juan.perez").password("una-clave-segura").firstName("Juan").lastName("Perez")
                .email("juan.perez@example.com").imageUrl("https://example.com/juan.png").langKey("es")
                .build();

        User user = userDtoMapper.toDomain(request);

        assertThat(user.getLogin()).isEqualTo("juan.perez");
        assertThat(user.getFirstName()).isEqualTo("Juan");
        assertThat(user.getLastName()).isEqualTo("Perez");
        assertThat(user.getEmail()).isEqualTo("juan.perez@example.com");
        assertThat(user.getImageUrl()).isEqualTo("https://example.com/juan.png");
        assertThat(user.getLangKey()).isEqualTo("es");
        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.getActivated()).isNull();
        assertThat(user.getAuthorities()).isEmpty();
    }

    @Test
    void nullInputReturnsNull() {
        assertThat(userDtoMapper.toDomain(null)).isNull();
    }
}
