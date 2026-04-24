package portifolio.deuquanto.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portifolio.deuquanto.dto.response.GroupDetailedResponse;
import portifolio.deuquanto.dto.MemberBalanceDTO;
import portifolio.deuquanto.dto.MemberDTO;
import portifolio.deuquanto.dto.request.CreateGroupRequest;
import portifolio.deuquanto.dto.response.GroupSummaryResponse;
import portifolio.deuquanto.entity.*;
import portifolio.deuquanto.entity.enums.GroupRole;
import portifolio.deuquanto.repository.GroupMemberRepository;
import portifolio.deuquanto.repository.GroupRepository;
import portifolio.deuquanto.repository.UserRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final BalanceService balanceService;

    public GroupService(GroupRepository groupRepository, UserRepository userRepository, GroupMemberRepository groupMemberRepository, BalanceService balanceService) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.balanceService = balanceService;
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

        validateUserIsNotMember(guestUser.getId(), groupId);

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

        validateUserIsNotMember(loggedUser.getId(), group.getId());

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

    private boolean isGroupMember(UUID userId, Long groupId){
        return groupMemberRepository.existsByUserIdAndGroupId(userId, groupId);
    }

    public void validateUserIsNotMember(UUID userId, Long groupId) {
        if (isGroupMember(userId, groupId)) {
            throw new RuntimeException("Você já faz parte deste grupo.");
        }
    }

    public void validateUserIsMember(UUID userId, Long groupId) {
        if (!isGroupMember(userId, groupId)) {
            throw new RuntimeException("Acesso negado: Você não é membro deste grupo.");
        }
    }

    @Transactional(readOnly = true)
    public List<GroupSummaryResponse> getMyGroupsSummary(UUID userId) {
        List<Group> myGroups = groupRepository.findAllByUserId(userId);

        return myGroups.stream()
                .map(group -> {
                    BigDecimal balance = balanceService.getIndividualBalance(group.getId(), userId);

                    BigDecimal finalBalance = (balance != null) ? balance : BigDecimal.ZERO;
                    return new GroupSummaryResponse(
                            group.getId(),
                            group.getTitle(),
                            group.getGroupMembers().size(),
                            group.isExpired(),
                            finalBalance
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupDetailedResponse getGroupDetails(UUID userId, Long groupId){
        validateUserIsMember(userId,groupId);
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Grupo não encontrado."));

        List<MemberDTO> membersList = group.getGroupMembers().stream()
                .map(gm -> new MemberDTO(
                        gm.getUser().getId(),
                        gm.getUser().getName(),
                        gm.getUser().getPixKey(),
                        gm.getRole()
                ))
                .toList();

        return new GroupDetailedResponse(
                group.getId(),
                group.getTitle(),
                group.getDescription(),
                group.getCreatedAt(),
                group.getExpiresAt(),
                group.isExpired(),
                membersList
        );

    }


    @Transactional
    public void updateGroup(Long groupId, CreateGroupRequest request){
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Grupo não encontrado."));

        group.setTitle(request.title());
        group.setDescription(request.description());
        if(request.expiresAt() != null){
            Instant instantExpiration = request.expiresAt()
                    .atTime(23,59,59)
                    .atZone(ZoneId.of("America/Sao_Paulo"))
                    .toInstant();
                    group.setExpiresAt(instantExpiration);
        }

        groupRepository.save(group);
    }

    @Transactional
    public void deleteGroup(UUID userId, Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Grupo não encontrado."));

        List<MemberBalanceDTO> balances = balanceService.getGroupBalances(userId, groupId);
        boolean hasUnsettledDebts = balances.stream()
                .anyMatch(b -> b.netBalance().compareTo(BigDecimal.ZERO) != 0);
        if (hasUnsettledDebts) {
            throw new RuntimeException("Não é possível apagar um grupo que ainda possui dívidas em aberto. Quitem os saldos primeiro.");
        }

        groupRepository.delete(group);
    }

    @Transactional
    public void leaveGroup(UUID userId, Long groupId){
        GroupMember currentMembership = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new RuntimeException("Você não faz parte deste grupo."));

        BigDecimal balance = balanceService.getIndividualBalance(groupId, userId);
        if (balance != null && balance.compareTo(BigDecimal.ZERO) != 0) {
            throw new RuntimeException("Você não pode sair de um grupo com saldo pendente (deve estar zerado).");
        }

        if (currentMembership.getRole() ==GroupRole.ADMIN) {
            List<GroupMember> successors = groupMemberRepository.findPotentialSuccessors(groupId, userId);
            if (!successors.isEmpty()) {
                GroupMember newAdmin = successors.getFirst();
                newAdmin.setRole(GroupRole.ADMIN);
                groupMemberRepository.save(newAdmin);
            } else {
                groupRepository.deleteById(groupId);
            }
        }

        groupMemberRepository.delete(currentMembership);
    }
}
