package ar.edu.um.prog2.quiroga.turnos;

/** Valores falsos para los tests (no son secretos reales). */
public final class TestSecrets {

    /** Base64 de 64 bytes, el mínimo de HS512. */
    public static final String JWT_SECRET =
            "Y2xhdmUtZGUtcHJ1ZWJhLWRlLWxvcy10ZXN0cy1kZS10dXJub3MtbnVuY2EtdXNhci1lbi1wcm9kdWNjaW9uIQ==";

    private TestSecrets() {
    }
}
