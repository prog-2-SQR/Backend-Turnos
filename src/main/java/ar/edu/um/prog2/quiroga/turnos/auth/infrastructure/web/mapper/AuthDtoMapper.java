package ar.edu.um.prog2.quiroga.turnos.auth.infrastructure.web.mapper;

import ar.edu.um.prog2.quiroga.turnos.auth.domain.model.AccessToken;
import ar.edu.um.prog2.quiroga.turnos.auth.infrastructure.web.dto.TokenResponse;
import org.springframework.stereotype.Component;

@Component
public class AuthDtoMapper {

    public TokenResponse toResponse(AccessToken token) {
        if (token == null) return null;
        return TokenResponse.builder()
                .idToken(token.getValue())
                .build();
    }
}
