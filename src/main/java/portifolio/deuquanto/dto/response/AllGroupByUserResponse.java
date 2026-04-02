package portifolio.deuquanto.dto.response;


import portifolio.deuquanto.entity.Group;
import portifolio.deuquanto.entity.GroupRole;

import java.time.Instant;

public record AllGroupByUserResponse(
        Long id,
        String title,
        String userRole,
        Integer totalMembers,
        Instant createdAt,
        Boolean isExpired
) {

    public static AllGroupByUserResponse from (Group group, GroupRole role){
        return new AllGroupByUserResponse(
                group.getId(),
                group.getTitle(),
                role.name(),
                group.getTotalMembers(),
                group.getCreatedAt(),
                group.getIsExpired()
        );
    }
}
