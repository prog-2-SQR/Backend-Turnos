package ar.edu.um.prog2.quiroga.turnos.user.domain.ports.out;

/** Calcula el hash de una contraseña. El algoritmo (Argon2id) lo elige la infraestructura. */
public interface PasswordHasher {

    String hash(String rawPassword);
}
