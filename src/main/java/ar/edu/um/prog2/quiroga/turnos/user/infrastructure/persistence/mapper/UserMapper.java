package ar.edu.um.prog2.quiroga.turnos.user.infrastructure.persistence.mapper;

import java.util.HashSet;

import ar.edu.um.prog2.quiroga.turnos.user.domain.model.User;
import ar.edu.um.prog2.quiroga.turnos.user.infrastructure.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserEntity toEntity(User domain) {
        if (domain == null) return null;
        return UserEntity.builder()
                .id(domain.getId())
                .login(domain.getLogin())
                .passwordHash(domain.getPasswordHash())
                .firstName(domain.getFirstName())
                .lastName(domain.getLastName())
                .email(domain.getEmail())
                .imageUrl(domain.getImageUrl())
                .activated(domain.getActivated())
                .langKey(domain.getLangKey())
                .authorities(new HashSet<>(domain.getAuthorities()))
                .createdBy(domain.getCreatedBy())
                .createdDate(domain.getCreatedDate())
                .lastModifiedBy(domain.getLastModifiedBy())
                .lastModifiedDate(domain.getLastModifiedDate())
                .build();
    }

    public User toDomain(UserEntity entity) {
        if (entity == null) return null;
        return User.builder()
                .id(entity.getId())
                .login(entity.getLogin())
                .passwordHash(entity.getPasswordHash())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .imageUrl(entity.getImageUrl())
                .activated(entity.getActivated())
                .langKey(entity.getLangKey())
                .authorities(new HashSet<>(entity.getAuthorities()))
                .createdBy(entity.getCreatedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedBy(entity.getLastModifiedBy())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }
}
