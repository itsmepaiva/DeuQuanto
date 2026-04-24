package portifolio.deuquanto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import portifolio.deuquanto.dto.request.ExpenseRequest;
import portifolio.deuquanto.entity.Expense;
import portifolio.deuquanto.entity.Group;
import portifolio.deuquanto.entity.GroupMember;
import portifolio.deuquanto.entity.Users;
import portifolio.deuquanto.entity.enums.ExpenseType;
import portifolio.deuquanto.repository.ExpenseRepository;
import portifolio.deuquanto.repository.GroupRepository;
import portifolio.deuquanto.repository.UserRepository;
import portifolio.deuquanto.service.ExpenseService;
import portifolio.deuquanto.service.GroupService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private GroupService groupService;

    @InjectMocks
    private ExpenseService expenseService;

    private UUID userId; // O "dono" da despesa
    private UUID otherUserId; // Alguém tentando invadir
    private Long groupId;
    private Long expenseId;

    private Users creator;
    private Group group;
    private Expense existingExpense;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        groupId = 1L;
        expenseId = 100L;

        creator = new Users();
        creator.setId(userId);
        creator.setName("Sávio");

        Users otherUser = new Users();
        otherUser.setId(otherUserId);
        otherUser.setName("Invasor");

        group = new Group();
        group.setId(groupId);
        group.setTitle("Férias");
        group.setExpiresAt(Instant.now().plusSeconds(88451710));

        GroupMember m1 = new GroupMember(); m1.setUser(creator);
        GroupMember m2 = new GroupMember(); m2.setUser(otherUser);
        group.setGroupMembers(new ArrayList<>(List.of(m1, m2)));

        existingExpense = new Expense();
        existingExpense.setId(expenseId);
        existingExpense.setTotalAmount(new BigDecimal("100.00"));
        existingExpense.setPaidBy(creator);
        existingExpense.setGroup(group);
        existingExpense.setSplits(new ArrayList<>()); // Lista de splits mutável

        lenient().when(userRepository.findById(userId)).thenReturn(Optional.of(creator));
        lenient().when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        lenient().when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(existingExpense));
        lenient().doNothing().when(groupService).validateUserIsMember(any(UUID.class), anyLong());
    }

    // ==========================================
    // 1. FUNCTION: CREATE EXPENSE
    // ==========================================
    @Test
    @DisplayName("Happy Path: Cria despesa com data informada e realiza os splits")
    void shouldCreateExpenseWithDateAndSplits() {
        ExpenseRequest request = new ExpenseRequest("MERCADO", "Compras", new BigDecimal("50.00"), LocalDate.now());
        lenient().when(expenseRepository.save(any(Expense.class))).thenAnswer(i -> i.getArguments()[0]);

        assertDoesNotThrow(() -> expenseService.createExpense(userId, groupId, request));

        Mockito.verify(expenseRepository).save(any(Expense.class));
    }

    @Test
    @DisplayName("Happy Path: Cria despesa sem data informada (Assume now)")
    void shouldCreateExpenseWithoutDate() {
        ExpenseRequest request = new ExpenseRequest("MERCADO", "Compras", new BigDecimal("50.00"), null);

        assertDoesNotThrow(() -> expenseService.createExpense(userId, groupId, request));
        Mockito.verify(expenseRepository).save(any(Expense.class));
    }

    @Test
    @DisplayName("Sad Path: Falha ao criar despesa em grupo expirado")
    void shouldThrowExceptionWhenCreatingInExpiredGroup() {
        group.setExpiresAt(Instant.now().minusSeconds(60));

        ExpenseRequest request = new ExpenseRequest("MERCADO", "Compras", new BigDecimal("50.00"), LocalDate.now());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> expenseService.createExpense(userId, groupId, request));
        assertEquals("O grupo escolhido está expirado", ex.getMessage());
    }

    // ==========================================
    // 2. FUNCTION: UPDATE EXPENSE
    // ==========================================
    @Test
    @DisplayName("Happy Path: Atualiza Descrição e Tipo (sem alterar o valor)")
    void shouldUpdateDescriptionAndType() {
        ExpenseRequest request = new ExpenseRequest("LAZER", "Cinema", new BigDecimal("100.00"), null);

        expenseService.updateExpense(userId, groupId, expenseId, request);

        assertEquals("Cinema", existingExpense.getDescription());
        assertEquals(ExpenseType.LAZER, existingExpense.getType());

        assertTrue(existingExpense.getSplits().isEmpty());
        Mockito.verify(expenseRepository).save(existingExpense);
    }

    @Test
    @DisplayName("Happy Path: Atualiza o Valor (Recria os splits)")
    void shouldUpdateAmountAndRecreateSplits() {
        // ARRANGE
        ExpenseRequest request = new ExpenseRequest(null, null, new BigDecimal("60.00"), null);

        GroupMember m1 = new GroupMember(); m1.setUser(creator);
        GroupMember m2 = new GroupMember(); m2.setUser(new Users());
        Mockito.when(groupService.getAllMembersGroups(userId)).thenReturn(List.of(m1, m2));

        expenseService.updateExpense(userId, groupId, expenseId, request);

        assertEquals(new BigDecimal("60.00"), existingExpense.getTotalAmount());
        assertEquals(2, existingExpense.getSplits().size());
        assertEquals(new BigDecimal("30.00"), existingExpense.getSplits().get(0).getAmountOwed());
    }

    @Test
    @DisplayName("Sad Path: Impede atualização se o usuário não for quem pagou")
    void shouldThrowExceptionWhenUpdaterIsNotThePayer() {
        ExpenseRequest request = new ExpenseRequest("LAZER", "Cinema", new BigDecimal("100.00"), null);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                expenseService.updateExpense(otherUserId, groupId, expenseId, request));

        assertEquals("Apenas quem pagou a conta pode editá-la.", ex.getMessage());
    }

    @Test
    @DisplayName("Sad Path: Impede atualização se a despesa for de outro grupo")
    void shouldThrowExceptionWhenExpenseBelongsToDifferentGroup() {
        ExpenseRequest request = new ExpenseRequest("LAZER", "Cinema", new BigDecimal("100.00"), null);
        Long wrongGroupId = 99L;

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                expenseService.updateExpense(userId, wrongGroupId, expenseId, request));

        assertEquals("Esta despesa nao pertence ao grupo informado", ex.getMessage());
    }

    // ==========================================
    // 3. FUNCTION: DELETE EXPENSE
    // ==========================================
    @Test
    @DisplayName("Happy Path: Deleta a despesa se o usuário for o pagador")
    void shouldDeleteExpenseSuccessfully() {
        assertDoesNotThrow(() -> expenseService.deleteExpense(userId, groupId, expenseId));

        Mockito.verify(expenseRepository).delete(existingExpense);
    }

    @Test
    @DisplayName("Sad Path: Impede deleção se o usuário não for o pagador")
    void shouldThrowExceptionWhenDeleterIsNotThePayer() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                expenseService.deleteExpense(otherUserId, groupId, expenseId));

        assertEquals("Apenas o usuário que registrou a despesa pode apagá-la.", ex.getMessage());
        Mockito.verify(expenseRepository, Mockito.never()).delete(any());
    }

    @Test
    @DisplayName("Sad Path: Impede deleção se a despesa for de outro grupo")
    void shouldThrowExceptionWhenDeletingExpenseFromDifferentGroup() {
        Long wrongGroupId = 99L;

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                expenseService.deleteExpense(userId, wrongGroupId, expenseId));

        assertEquals("Esta despesa não pertence ao grupo informado.", ex.getMessage());
        Mockito.verify(expenseRepository, Mockito.never()).delete(any());
    }
}