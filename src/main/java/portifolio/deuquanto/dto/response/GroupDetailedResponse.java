package portifolio.deuquanto.dto.response;

import portifolio.deuquanto.dto.MemberDTO;

import java.time.Instant;
import java.util.List;

public record GroupDetailedResponse(
        Long id,
        String title,
        String description,
        Instant createdAt,
        Instant expiresAt,
        boolean isExpired,
        List<MemberDTO> members
) {
}
