package portifolio.deuquanto.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RegisterUserRequest(@NotBlank(message = "O nome do usuario é obrigatório") String name,
                                  @NotBlank(message = "O email é obrigatório") String email,
                                  @NotBlank(message = "A senha é obrigatória") String password,
                                  @NotBlank(message = "A chave pix é obrigatória") String pixKey) {
}
