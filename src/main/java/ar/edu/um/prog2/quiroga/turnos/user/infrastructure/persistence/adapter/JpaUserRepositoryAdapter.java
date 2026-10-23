package ar.edu.um.prog2.quiroga.turnos.user.infrastructure.persistence.adapter;

import ar.edu.um.prog2.quiroga.turnos.user.domain.model.User;
import ar.edu.um.prog2.quiroga.turnos.user.domain.ports.out.UserRepository;
import ar.edu.um.prog2.quiroga.turnos.user.infrastructure.persistence.entity.UserEntity;
import ar.edu.um.prog2.quiroga.turnos.user.infrastructure.persistence.mapper.UserMapper;
import ar.edu.um.prog2.quiroga.turnos.user.infrastructure.persistence.repository.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaUserRepositoryAdapter implements UserRepository {

    private final JpaUserRepository jpaUserRepository;
    private final UserMapper userMapper;

    @Override
    public boolean existsUserByLogin(String login) {
        return jpaUserRepository.existsByLogin(login);
    }

    @Override
    public boolean existsUserByEmail(String email) {
        return jpaUserRepository.existsByEmail(email);
    }

    @Override
    public User saveUser(User user) {
        UserEntity saved = jpaUserRepository.save(userMapper.toEntity(user));
        return userMapper.toDomain(saved);
    }
}
