package ar.edu.um.prog2.quiroga.turnos.user.infrastructure.web.mapper;

import ar.edu.um.prog2.quiroga.turnos.user.domain.model.User;
import ar.edu.um.prog2.quiroga.turnos.user.infrastructure.web.dto.RegisterUserRequest;
import org.springframework.stereotype.Component;

/** Solo copia lo que el usuario puede elegir: id, activación, roles y auditoría los pone el backend. */
@Component
public class UserDtoMapper {

    public User toDomain(RegisterUserRequest request) {
        if (request == null) return null;
        return User.builder()
                .login(request.getLogin())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .imageUrl(request.getImageUrl())
                .langKey(request.getLangKey())
                .build();
    }
}
