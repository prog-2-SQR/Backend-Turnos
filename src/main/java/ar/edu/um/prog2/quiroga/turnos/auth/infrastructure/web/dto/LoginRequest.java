package ar.edu.um.prog2.quiroga.turnos.auth.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Body de POST /api/authenticate (el LoginVM de JHipster). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank
    @Size(max = 50)
    private String username;
    @NotBlank
    @Size(min = 4, max = 100)
    private String password;
    // Boolean y no boolean: es opcional, y Jackson 3 no acepta null en un primitivo.
    private Boolean rememberMe;
}
