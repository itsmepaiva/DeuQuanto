package portifolio.deuquanto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import portifolio.deuquanto.dto.MemberBalanceDTO;
import portifolio.deuquanto.dto.request.CreateGroupRequest;
import portifolio.deuquanto.dto.response.GroupDetailedResponse;
import portifolio.deuquanto.dto.response.GroupSummaryResponse;
import portifolio.deuquanto.entity.Group;
import portifolio.deuquanto.entity.GroupMember;
import portifolio.deuquanto.entity.Users;
import portifolio.deuquanto.entity.enums.GroupRole;
import portifolio.deuquanto.repository.GroupMemberRepository;
import portifolio.deuquanto.repository.GroupRepository;
import portifolio.deuquanto.repository.UserRepository;
import portifolio.deuquanto.service.BalanceService;
import portifolio.deuquanto.service.GroupService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock private GroupRepository groupRepository;
    @Mock private GroupMemberRepository groupMemberRepository;
    @Mock private BalanceService balanceService;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private GroupService groupService;

    private UUID userId;
    private UUID inviteeId;
    private Long groupId;
    private Group group;
    private Users currentUser;
    private Users inviteeUser;
    private GroupMember adminMember;
    private GroupMember normalMember;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        inviteeId = UUID.randomUUID();
        groupId = 1L;

        currentUser = new Users();
        currentUser.setId(userId);
        currentUser.setName("Admin");

        inviteeUser = new Users();
        inviteeUser.setId(inviteeId);
        inviteeUser.setName("Convidado");

        group = new Group();
        group.setId(groupId);
        group.setTitle("Viagem Férias");
        group.setInviteToken("vG8kLp");
        group.setExpiresAt(Instant.now().plusSeconds(84471510));

        adminMember = new GroupMember();
        adminMember.setGroup(group);
        adminMember.setRole(GroupRole.ADMIN);
        adminMember.setUser(currentUser);

        normalMember = new GroupMember();
        normalMember.setGroup(group);
        normalMember.setRole(GroupRole.MEMBER);
        normalMember.setUser(inviteeUser);

        currentUser.setMembers(new java.util.ArrayList<>(List.of(adminMember)));
        group.setGroupMembers(new java.util.ArrayList<>(List.of(adminMember, normalMember)));

        lenient().when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));
        lenient().when(userRepository.findById(inviteeId)).thenReturn(Optional.of(inviteeUser));
        lenient().when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));

        lenient().when(groupMemberRepository.findAllByUserIdWithGroups(userId)).thenReturn(List.of(adminMember));
        lenient().when(groupMemberRepository.existsByUserIdAndGroupId(userId, groupId)).thenReturn(true);
        lenient().when(groupRepository.findAllByUserId(any(UUID.class))).thenReturn(List.of(group));
    }

    // ==========================================
    // 1. FUNCTION: CRIAR GRUPO
    // ==========================================
    @Test
    @DisplayName("Happy Path: Deve criar grupo e definir criador como ADMIN")
    void shouldCreateGroupSuccessfully() {
        CreateGroupRequest request = new CreateGroupRequest("Viagem", "Férias", LocalDate.now().plusDays(5));
        lenient().when(groupRepository.save(any(Group.class))).thenReturn(group);

        Group createdGroup = groupService.createGroup(userId, request);

        assertNotNull(createdGroup);
    }


    // ==========================================
    // 2. FUNCTION: ADICIONAR MEMBRO
    // ==========================================
    @Test
    @DisplayName("Happy Path: Add Membro com sucesso")
    void shouldAddMemberSuccessfully() {
        lenient().when(userRepository.findByEmail("amigo@email.com")).thenReturn(Optional.of(inviteeUser));
        lenient().when(groupMemberRepository.existsByUserIdAndGroupId(inviteeId, groupId)).thenReturn(false);

        assertDoesNotThrow(() -> groupService.addMember(groupId, "amigo@email.com"));

    }

    @Test
    @DisplayName("Sad Path: Falha se o convidado já estiver no grupo")
    void shouldThrowExceptionWhenInviteeIsAlreadyInGroup() {
        lenient().when(userRepository.findByEmail("amigo@email.com")).thenReturn(Optional.of(inviteeUser));
        lenient().when(groupMemberRepository.existsByUserIdAndGroupId(inviteeId, groupId)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> groupService.addMember(groupId, "amigo@email.com"));
    }

    // ==========================================
    // 3. FUNCTION: ENTRAR NO GRUPO COM CONVITE
    // ==========================================
    @Test
    @DisplayName("Happy Path: Entra no grupo com código válido")
    void shouldJoinGroupWithValidCode() {
        lenient().when(groupRepository.findByInviteToken("vG8kLp")).thenReturn(Optional.of(group));
        lenient().when(groupMemberRepository.existsByUserIdAndGroupId(userId, groupId)).thenReturn(false);

        assertDoesNotThrow(() -> groupService.joinGroupWithCode(userId, "vG8kLp"));
    }

    @Test
    @DisplayName("Sad Path: Falha ao entrar com código inexistente")
    void shouldThrowExceptionWhenInviteCodeIsInvalid() {
        lenient().when(groupRepository.findByInviteToken("ERRADO")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> groupService.joinGroupWithCode(userId, "ERRADO"));
    }

    @Test
    @DisplayName("Sad Path: Falha se usuário já pertence ao grupo")
    void shouldThrowExceptionWhenJoiningButAlreadyMember() {
        lenient().when(groupRepository.findByInviteToken("vG8kLp")).thenReturn(Optional.of(group));
        lenient().when(groupMemberRepository.existsByUserIdAndGroupId(userId, groupId)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> groupService.joinGroupWithCode(userId, "vG8kLp"));
    }

    // ==========================================
    // 4. FUNCTION: ATUALIZAR DADOS DO GRUPO
    // ==========================================
    @Test
    @DisplayName("Happy Path: Atualiza dados com sucesso")
    void shouldUpdateGroupDataSuccessfully() {
        CreateGroupRequest request = new CreateGroupRequest("Novo Título", "Nova Desc", null);
        groupService.updateGroup(groupId, request);

        assertEquals("Novo Título", group.getTitle());
        Mockito.verify(groupRepository).save(group);
    }

    // ==========================================
    // 5. FUNCTION: SAIR DO GRUPO
    // ==========================================
    @Test
    @DisplayName("Happy Path: Membro sai do grupo sem saldo pendente")
    void shouldLeaveGroupSuccessfullyWithNoBalance() {
        lenient().when(groupMemberRepository.findByGroupIdAndUserId(groupId, userId)).thenReturn(Optional.of(adminMember));
        lenient().when(balanceService.getIndividualBalance(groupId, userId)).thenReturn(BigDecimal.ZERO);
        lenient().when(groupMemberRepository.findPotentialSuccessors(groupId, userId)).thenReturn(List.of(normalMember));

        groupService.leaveGroup(userId, groupId);

        Mockito.verify(groupMemberRepository).delete(adminMember);
    }

    @Test
    @DisplayName("Sad Path: Tenta sair com dívida pendente")
    void shouldThrowExceptionWhenLeavingWithPendingBalance() {
        lenient().when(groupMemberRepository.findByGroupIdAndUserId(groupId, userId)).thenReturn(Optional.of(adminMember));
        lenient().when(balanceService.getIndividualBalance(groupId, userId)).thenReturn(new BigDecimal("-50.00"));

        assertThrows(RuntimeException.class, () -> groupService.leaveGroup(userId, groupId));
    }

    // ==========================================
    // 6. FUNCTION: DELETAR O GRUPO
    // ==========================================
    @Test
    @DisplayName("Happy Path: Deleta o grupo sem saldos pendentes no grupo inteiro")
    void shouldDeleteGroupSuccessfully() {
        MemberBalanceDTO zeroBalanceMember = new MemberBalanceDTO(userId, "Test", BigDecimal.ZERO, null);
        lenient().when(balanceService.getGroupBalances(userId, groupId)).thenReturn(List.of(zeroBalanceMember));

        groupService.deleteGroup(userId, groupId);

        Mockito.verify(groupRepository).delete(any(Group.class));
        }

    @Test
    @DisplayName("Sad Path: Bloqueia deleção se houver saldo pendente no grupo")
    void shouldThrowExceptionWhenDeletingWithPositiveBalance() {
        MemberBalanceDTO debtMember = new MemberBalanceDTO(userId, "Sávio", new BigDecimal("10.00"), null);
        lenient().when(balanceService.getGroupBalances(userId, groupId)).thenReturn(List.of(debtMember));

        assertThrows(RuntimeException.class, () -> groupService.deleteGroup(userId, groupId));
        Mockito.verify(groupRepository, Mockito.never()).deleteById(any());
    }

    // ==========================================
    // 7. FUNCTION: GET INVITE TOKEN
    // ==========================================
    @Test
    @DisplayName("Happy Path: Retorna token")
    void shouldReturnInviteToken() {
        String token = groupService.getInviteToken(groupId); // Parâmetro adaptado
        assertEquals("vG8kLp", token);
    }

    // ==========================================
    // 8. FUNCTION: GET MY GROUP SUMMARY
    // ==========================================
    @Test
    @DisplayName("Happy Path: Retorna resumo com balanço calculado")
    void shouldReturnSummaryWithCalculatedBalances() {
        lenient().when(balanceService.getIndividualBalance(anyLong(), any(UUID.class))).thenReturn(new BigDecimal("25.00"));

        List<GroupSummaryResponse> result = groupService.getMyGroupsSummary(userId);

        assertFalse(result.isEmpty(), "A lista de grupos não deveria estar vazia.");
        assertEquals(new BigDecimal("25.00"), result.get(0).balance());
    }

    @Test
    @DisplayName("Edge Case: Grupo é marcado como expirado corretamente")
    void shouldCorrectlyFlagGroupAsExpiredAfterMidnight() {
        group.setExpiresAt(Instant.now().minusSeconds(60));
        lenient().when(balanceService.getIndividualBalance(anyLong(), any(UUID.class))).thenReturn(BigDecimal.ZERO);

        List<GroupSummaryResponse> result = groupService.getMyGroupsSummary(userId);

        assertFalse(result.isEmpty(), "A lista de grupos não deveria estar vazia.");
        assertTrue(result.get(0).isExpired());
    }

    // ==========================================
    // 9. FUNCTION: GROUP DETAILS
    // ==========================================
    @Test
    @DisplayName("Happy Path: Retorna o DTO gigante do grupo perfeitamente")
    void shouldReturnGroupDetailsSuccessfully() {
        lenient().when(groupMemberRepository.findByGroupIdAndUserId(groupId, userId)).thenReturn(Optional.of(adminMember));
        GroupDetailedResponse details = groupService.getGroupDetails(userId, groupId);

        assertNotNull(details);
        assertEquals("Viagem Férias", details.title());
        assertEquals(2, details.members().size());
        assertEquals("Admin", details.members().get(0).name());
    }
}