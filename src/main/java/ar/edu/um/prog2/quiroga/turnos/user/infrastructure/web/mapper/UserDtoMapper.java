package ar.edu.um.prog2.quiroga.turnos.user.infrastructure.web.mapper;

import java.util.Set;

import ar.edu.um.prog2.quiroga.turnos.user.domain.model.User;
import ar.edu.um.prog2.quiroga.turnos.user.infrastructure.web.dto.AccountResponse;
import ar.edu.um.prog2.quiroga.turnos.user.infrastructure.web.dto.RegisterUserRequest;
import org.springframework.stereotype.Component;

/** Al registrar solo copia lo que el usuario puede elegir; al responder, nunca expone el hash de la contraseña. */
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

    public AccountResponse toAccountResponse(User domain) {
        if (domain == null) return null;
        return AccountResponse.builder()
                .id(domain.getId())
                .login(domain.getLogin())
                .firstName(domain.getFirstName())
                .lastName(domain.getLastName())
                .email(domain.getEmail())
                .imageUrl(domain.getImageUrl())
                .activated(domain.getActivated())
                .langKey(domain.getLangKey())
                .authorities(Set.copyOf(domain.getAuthorities()))
                .createdBy(domain.getCreatedBy())
                .createdDate(domain.getCreatedDate())
                .lastModifiedBy(domain.getLastModifiedBy())
                .lastModifiedDate(domain.getLastModifiedDate())
                .build();
    }
}
