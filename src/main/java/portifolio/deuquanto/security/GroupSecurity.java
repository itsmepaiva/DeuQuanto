package portifolio.deuquanto.security;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import portifolio.deuquanto.entity.GroupMemberId;
import portifolio.deuquanto.entity.GroupRole;
import portifolio.deuquanto.repository.GroupMemberRepository;

import java.util.UUID;

@Component("groupSecurity")
public class GroupSecurity {

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    public boolean isGroupAdmin(UUID userId, Long groupId){
        return groupMemberRepository.findById(new GroupMemberId(userId, groupId))
                .map(member -> member.getRole() == GroupRole.ADMIN)
                .orElse(false);
    }
}
