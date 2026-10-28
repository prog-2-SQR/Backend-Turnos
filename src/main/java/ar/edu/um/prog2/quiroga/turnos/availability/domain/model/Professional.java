package ar.edu.um.prog2.quiroga.turnos.availability.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Lo que Turnos necesita saber de un profesional del catálogo. No se guarda: se consulta en cada operación. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Professional {

    private Long id;
    private Long categoryId;
    private String firstName;
    private String lastName;
    private Boolean enabled;

    // Deshabilitado = baja lógica (anexo §7): no se ofrecen turnos ni se inician reservas.
    public boolean isAcceptingAppointments() {
        return Boolean.TRUE.equals(enabled);
    }
}
