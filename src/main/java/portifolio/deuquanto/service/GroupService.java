package portifolio.deuquanto.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import portifolio.deuquanto.dto.request.CreateGroupRequest;
import portifolio.deuquanto.entity.*;
import portifolio.deuquanto.repository.GroupMemberRepository;
import portifolio.deuquanto.repository.GroupRepository;
import portifolio.deuquanto.repository.UserRepository;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;

    public GroupService(GroupRepository groupRepository, UserRepository userRepository, GroupMemberRepository groupMemberRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.groupMemberRepository = groupMemberRepository;
    }


    @Transactional
    public Group createGroup(UUID creatorId, CreateGroupRequest request){
        Users creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Usuario nao encontrado!"));

        Group newGroup = new Group();
        newGroup.setTitle(request.title());
        if(request.expiresAt() != null){
            Instant instantExpiration = request.expiresAt()
                    .atTime(23,59,59)
                    .atZone(ZoneId.of("America/Sao_Paulo"))
                    .toInstant();
            newGroup.setExpiresAt(instantExpiration);
        }
        newGroup.setDescription(request.description());


        GroupMember membership = new GroupMember();
        membership.setUser(creator);
        membership.setGroup(newGroup);
        membership.setRole(GroupRole.ADMIN);

        newGroup.getGroupMembers().add(membership);

        return groupRepository.save(newGroup);
    }

    public List<GroupMember> getAllMembersGroups(UUID userId){
        return groupMemberRepository.findAllByUserIdWithGroups(userId);
    }

    @Transactional
    public void addMember(Long groupId, String guestEmail){
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Grupo nao encontrado"));
        Users guestUser = userRepository.findByEmail(guestEmail)
                .orElseThrow(() -> new RuntimeException("Usuario nao encontrado"));

        isGroupMember(guestUser.getId(), groupId);

        GroupMember newMembership = createMembership(guestUser, group);
        group.getGroupMembers().add(newMembership);
        groupRepository.save(group);
    }

    public String getInviteToken(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Grupo nao encontrado"));
        return group.getInviteToken();
    }

    @Transactional
    public GroupMember joinGroupWithCode(UUID userId, String inviteToken) {
        Group group = groupRepository.findByInviteToken(inviteToken)
                .orElseThrow(() -> new RuntimeException("Grupo nao encontrado"));
        Users loggedUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario nao encontrado"));

        isGroupMember(loggedUser.getId(), group.getId());

        GroupMember newMembership = createMembership(loggedUser, group);
        group.getGroupMembers().add(newMembership);
        groupRepository.save(group);
        return newMembership;
    }

    private GroupMember createMembership(Users user, Group group){
        GroupMember newMembership = new GroupMember();
        newMembership.setUser(user);
        newMembership.setGroup(group);
        newMembership.setRole(GroupRole.MEMBER);
        return newMembership;
    }

    public void isGroupMember(UUID userId, Long groupId){
        GroupMemberId groupMemberId = new GroupMemberId(userId, groupId);
        if (groupMemberRepository.existsById(groupMemberId)){
            throw new RuntimeException("Este usuario ja é membro deste grupo");
        }
    }


}
