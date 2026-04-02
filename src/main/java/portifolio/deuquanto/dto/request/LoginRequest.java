package portifolio.deuquanto.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank(message = "O email é obrigatório para o login") String email,
                           @NotBlank(message = "A senha é obrigatória para o login") String password) {
}
