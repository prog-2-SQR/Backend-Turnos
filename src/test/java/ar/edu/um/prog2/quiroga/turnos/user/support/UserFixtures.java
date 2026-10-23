package ar.edu.um.prog2.quiroga.turnos.user.support;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import ar.edu.um.prog2.quiroga.turnos.user.domain.model.Authorities;
import ar.edu.um.prog2.quiroga.turnos.user.domain.model.User;

/** Datos de prueba del slice de usuarios. */
public final class UserFixtures {

    public static final Instant CREATED = Instant.parse("2026-10-23T12:00:00Z");
    // Hash BCrypt de ejemplo (60 caracteres), solo para que la columna tenga un valor con el largo real.
    public static final String HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5BWX4Z3Z0kq0E9tq7Rl1Xc5u5gQ2W";

    private UserFixtures() {
    }

    /** Lo que llega del registro: solo los datos que elige el usuario. */
    public static User registration() {
        return User.builder()
                .login("Juan.Perez")
                .firstName("Juan")
                .lastName("Perez")
                .email("Juan.Perez@Example.com")
                .imageUrl("https://example.com/juan.png")
                .build();
    }

    /** Un usuario ya registrado, como lo guarda el backend. */
    public static User juan() {
        return User.builder()
                .login("juan.perez")
                .passwordHash(HASH)
                .firstName("Juan")
                .lastName("Perez")
                .email("juan.perez@example.com")
                .imageUrl("https://example.com/juan.png")
                .activated(true)
                .langKey("es")
                .authorities(new HashSet<>(Set.of(Authorities.USER)))
                .createdBy("system")
                .createdDate(CREATED)
                .lastModifiedBy("system")
                .lastModifiedDate(CREATED)
                .build();
    }
}
