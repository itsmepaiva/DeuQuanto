package portifolio.deuquanto.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddMemberRequest(@NotBlank(message = "O email nao pode estar vazio") String email) {
}
