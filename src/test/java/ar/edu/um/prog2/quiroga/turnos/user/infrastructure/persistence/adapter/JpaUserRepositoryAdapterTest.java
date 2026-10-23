package ar.edu.um.prog2.quiroga.turnos.user.infrastructure.persistence.adapter;

import static ar.edu.um.prog2.quiroga.turnos.user.support.UserFixtures.juan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.um.prog2.quiroga.turnos.TestcontainersConfiguration;
import ar.edu.um.prog2.quiroga.turnos.user.domain.model.Authorities;
import ar.edu.um.prog2.quiroga.turnos.user.domain.model.User;
import ar.edu.um.prog2.quiroga.turnos.user.infrastructure.persistence.mapper.UserMapper;
import ar.edu.um.prog2.quiroga.turnos.user.infrastructure.persistence.repository.JpaUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/** Adapter contra un PostgreSQL real (Testcontainers) con las tablas jhi_* creadas por Liquibase. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaUserRepositoryAdapter.class, UserMapper.class})
class JpaUserRepositoryAdapterTest {

    @Autowired
    private JpaUserRepositoryAdapter adapter;

    @Autowired
    private JpaUserRepository jpaUserRepository;

    @Test
    void saveGeneratesTheIdAndKeepsTheAuthorities() {
        User saved = adapter.saveUser(juan());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAuthorities()).containsExactly(Authorities.USER);
        assertThat(saved.getPasswordHash()).hasSize(60);
    }

    @Test
    void existsByLoginAndEmail() {
        adapter.saveUser(juan());

        assertThat(adapter.existsUserByLogin("juan.perez")).isTrue();
        assertThat(adapter.existsUserByLogin("otro")).isFalse();
        assertThat(adapter.existsUserByEmail("juan.perez@example.com")).isTrue();
        assertThat(adapter.existsUserByEmail("otro@example.com")).isFalse();
    }

    @Test
    void theDatabaseRejectsADuplicatedLogin() {
        adapter.saveUser(juan());
        User duplicated = juan();
        duplicated.setEmail("otro@example.com");

        assertThatThrownBy(() -> adapter.saveUser(duplicated))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theDatabaseRejectsAnUnknownAuthority() {
        User user = juan();
        user.getAuthorities().add("ROLE_INVENTADO");

        // Los roles se insertan al hacer flush (fin de la transacción); se fuerza acá para ver la FK.
        assertThatThrownBy(() -> {
            adapter.saveUser(user);
            jpaUserRepository.flush();
        })
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
