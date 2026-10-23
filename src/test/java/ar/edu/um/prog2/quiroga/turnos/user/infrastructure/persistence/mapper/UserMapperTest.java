package ar.edu.um.prog2.quiroga.turnos.user.infrastructure.persistence.mapper;

import static ar.edu.um.prog2.quiroga.turnos.user.support.UserFixtures.juan;
import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.um.prog2.quiroga.turnos.user.domain.model.User;
import org.junit.jupiter.api.Test;

class UserMapperTest {

    private final UserMapper userMapper = new UserMapper();

    @Test
    void roundTripKeepsAllFields() {
        User user = juan();
        user.setId(5L);

        assertThat(userMapper.toDomain(userMapper.toEntity(user)))
                .usingRecursiveComparison()
                .isEqualTo(user);
    }

    @Test
    void nullInputsReturnNull() {
        assertThat(userMapper.toEntity(null)).isNull();
        assertThat(userMapper.toDomain(null)).isNull();
    }
}
