package portifolio.deuquanto.dto;

import java.util.UUID;

public record UserProfileDTO(
        UUID id,
        String name,
        String email,
        String pixKey
) {
}
