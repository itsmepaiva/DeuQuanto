package portifolio.deuquanto.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "O e-mail não pode estar vazio.")
        @Email(message = "Formato de e-mail inválido.")
        String email
) {
}
