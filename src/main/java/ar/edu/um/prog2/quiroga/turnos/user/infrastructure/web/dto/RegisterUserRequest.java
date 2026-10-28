package ar.edu.um.prog2.quiroga.turnos.user.infrastructure.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body de POST /api/register (el ManagedUserVM de JHipster). Límites de la cátedra (anexo §5.1) y regex de login de
 * JHipster. La contraseña no se valida acá: la valida el caso de uso para responder INVALID_PASSWORD (anexo §13.1).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterUserRequest {

    // Login de JHipster: un email o letras, números y _.@-. Las dos alternativas van agrupadas para que ^ y $ apliquen
    // a las dos (en el original, ^A|B$ se lee (^A)|(B$)), y la repetición es posesiva (*+) para no acumular
    // retrocesos con textos largos. Acepta exactamente los mismos logins que antes (LoginRegexTest).
    public static final String LOGIN_REGEX =
            "^(?:(?>[a-zA-Z0-9!$&*+=?^_`{|}~.-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*+)|(?>[_.@A-Za-z0-9-]+))$";

    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = LOGIN_REGEX)
    private String login;
    private String password;
    @Size(max = 50)
    private String firstName;
    @Size(max = 50)
    private String lastName;
    @Email
    @Size(min = 5, max = 254)
    private String email;
    @Size(max = 256)
    private String imageUrl;
    @Size(min = 2, max = 10)
    private String langKey;
}
