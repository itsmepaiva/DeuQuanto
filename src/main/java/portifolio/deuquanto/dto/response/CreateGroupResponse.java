package portifolio.deuquanto.dto.response;

import portifolio.deuquanto.entity.Group;
import portifolio.deuquanto.entity.enums.GroupRole;

import java.time.Instant;

public record CreateGroupResponse(Long id, String title, String userRole, Instant createdAt, Instant expiresAt, String description) {

    public static CreateGroupResponse from (Group group, GroupRole role){
        return new CreateGroupResponse(
                group.getId(),
                group.getTitle(),
                role.name(),
                group.getCreatedAt(),
                group.getExpiresAt(),
                group.getDescription()
        );
    }
}
