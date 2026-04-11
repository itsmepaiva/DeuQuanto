package portifolio.deuquanto.dto;

import portifolio.deuquanto.entity.enums.GroupRole;

import java.util.UUID;

public record MemberDTO(
        UUID id,
        String name,
        String pixKey,
        GroupRole role
) {
}
