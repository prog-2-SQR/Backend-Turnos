package ar.edu.um.prog2.quiroga.turnos.user.infrastructure.persistence.repository;

import java.util.Optional;

import ar.edu.um.prog2.quiroga.turnos.user.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaUserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findOneByLogin(String login);

    boolean existsByLogin(String login);

    boolean existsByEmail(String email);
}
