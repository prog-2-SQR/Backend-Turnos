package ar.edu.um.prog2.quiroga.turnos.auth.infrastructure.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.um.prog2.quiroga.turnos.auth.domain.model.AccessToken;
import org.junit.jupiter.api.Test;

class AuthDtoMapperTest {

    private final AuthDtoMapper authDtoMapper = new AuthDtoMapper();

    @Test
    void toResponseExposesOnlyTheToken() {
        assertThat(authDtoMapper.toResponse(AccessToken.builder().value("token").build()).getIdToken()).isEqualTo("token");
    }

    @Test
    void nullInputReturnsNull() {
        assertThat(authDtoMapper.toResponse(null)).isNull();
    }
}
