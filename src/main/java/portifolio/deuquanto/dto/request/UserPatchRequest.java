package portifolio.deuquanto.dto.request;

import jakarta.validation.constraints.Size;

public record UserPatchRequest(
        @Size(min = 2, message = "O nome deve ter pelo menos 2 caracteres.")
        String name,
        String pixKey
) {
}
