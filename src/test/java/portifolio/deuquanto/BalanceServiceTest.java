package portifolio.deuquanto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import portifolio.deuquanto.dto.GroupActivityDTO;
import portifolio.deuquanto.dto.IndividualBalanceDTO;
import portifolio.deuquanto.dto.MemberBalanceDTO;
import portifolio.deuquanto.dto.response.SuggestedPaymentResponse;
import portifolio.deuquanto.entity.*;
import portifolio.deuquanto.entity.enums.ActivityType;
import portifolio.deuquanto.entity.enums.BalanceStatus;
import portifolio.deuquanto.entity.enums.ExpenseType;
import portifolio.deuquanto.repository.*;
import portifolio.deuquanto.service.BalanceService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock private ExpenseRepository expenseRepository;
    @Mock private ExpenseSplitRepository expenseSplitRepository;
    @Mock private SettlementRepository settlementRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private GroupMemberRepository groupMemberRepository;

    @InjectMocks
    private BalanceService balanceService;

    private UUID userId;
    private UUID userB_Id;
    private UUID userC_Id;
    private Long groupId;
    private Group group;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userB_Id = UUID.randomUUID();
        userC_Id = UUID.randomUUID();
        groupId = 1L;

        // Criando o grupo e os membros para os testes de Agregação
        group = new Group();
        group.setId(groupId);
        group.setTitle("Viagem");

        Users userA = new Users();
        userA.setId(userId);
        userA.setName("Alice");

        Users userB = new Users();
        userB.setId(userB_Id);
        userB.setName("Bob");

        Users userC = new Users();
        userC.setId(userC_Id);
        userC.setName("Carlos");

        GroupMember memberA = new GroupMember(); memberA.setUser(userA); memberA.setGroup(group);
        GroupMember memberB = new GroupMember(); memberB.setUser(userB); memberB.setGroup(group);
        GroupMember memberC = new GroupMember(); memberC.setUser(userC); memberC.setGroup(group);

        group.setGroupMembers(new java.util.ArrayList<>(List.of(memberA, memberB, memberC)));

        lenient().when(groupMemberRepository.existsByUserIdAndGroupId(any(UUID.class), anyLong())).thenReturn(true);
        lenient().when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
    }

    // ==========================================
    // 1. FUNCTION: GET INDIVIDUAL BALANCE (O MOTOR)
    // ==========================================

    @Test
    @DisplayName("Happy Path: Usuário tem mais créditos que débitos (Saldo Positivo)")
    void shouldCalculatePositiveBalance() {
        Mockito.when(expenseRepository.sumTotalPaidByUser(groupId, userId)).thenReturn(new BigDecimal("100.00"));
        Mockito.when(settlementRepository.sumTotalSentByUser(groupId, userId)).thenReturn(new BigDecimal("50.00"));

        Mockito.when(expenseSplitRepository.sumTotalOwedByUser(groupId, userId)).thenReturn(new BigDecimal("80.00"));
        Mockito.when(settlementRepository.sumTotalReceivedByUser(groupId, userId)).thenReturn(new BigDecimal("20.00"));

        BigDecimal balance = balanceService.getIndividualBalance(groupId, userId);

        assertEquals(new BigDecimal("50.00"), balance);
    }

    @Test
    @DisplayName("Happy Path: Usuário tem mais débitos que créditos (Saldo Negativo)")
    void shouldCalculateNegativeBalance() {
        Mockito.when(expenseRepository.sumTotalPaidByUser(groupId, userId)).thenReturn(new BigDecimal("10.00"));
        Mockito.when(settlementRepository.sumTotalSentByUser(groupId, userId)).thenReturn(BigDecimal.ZERO);

        Mockito.when(expenseSplitRepository.sumTotalOwedByUser(groupId, userId)).thenReturn(new BigDecimal("60.00"));
        Mockito.when(settlementRepository.sumTotalReceivedByUser(groupId, userId)).thenReturn(BigDecimal.ZERO);

        BigDecimal balance = balanceService.getIndividualBalance(groupId, userId);

        assertEquals(new BigDecimal("-50.00"), balance);
    }

    @Test
    @DisplayName("Edge Case (Estado Zero): Sem gastos ou dívidas (Saldo Zero)")
    void shouldCalculateZeroBalanceWhenNoActivity() {
        Mockito.when(expenseRepository.sumTotalPaidByUser(groupId, userId)).thenReturn(BigDecimal.ZERO);
        Mockito.when(expenseSplitRepository.sumTotalOwedByUser(groupId, userId)).thenReturn(BigDecimal.ZERO);
        Mockito.when(settlementRepository.sumTotalSentByUser(groupId, userId)).thenReturn(BigDecimal.ZERO);
        Mockito.when(settlementRepository.sumTotalReceivedByUser(groupId, userId)).thenReturn(BigDecimal.ZERO);

        BigDecimal balance = balanceService.getIndividualBalance(groupId, userId);

        assertEquals(BigDecimal.ZERO, balance);
    }

    @Test
    @DisplayName("Edge Case Crítico (Segurança SQL): Protege contra o 'null' do banco de dados")
    void shouldHandleNullReturnsFromDatabase() {
        // ARRANGE: Simulando o comportamento real do SQL (retornar NULL quando não acha a pessoa na tabela)
        Mockito.when(expenseRepository.sumTotalPaidByUser(groupId, userId)).thenReturn(null);
        Mockito.when(expenseSplitRepository.sumTotalOwedByUser(groupId, userId)).thenReturn(null);
        Mockito.when(settlementRepository.sumTotalSentByUser(groupId, userId)).thenReturn(null);
        Mockito.when(settlementRepository.sumTotalReceivedByUser(groupId, userId)).thenReturn(null);

        BigDecimal balance = balanceService.getIndividualBalance(groupId, userId);

        assertEquals(BigDecimal.ZERO, balance);
    }

    // ==========================================
    // 2. FUNCTION: GET GROUP BALANCES (Agregação)
    // ==========================================
    @Test
    @DisplayName("Happy Path: Retorna lista com saldo de todos os membros do grupo")
    void shouldReturnBalancesForAllMembers() {
        lenient().when(expenseRepository.sumTotalPaidByUser(groupId, userId)).thenReturn(new BigDecimal("100.00"));
        lenient().when(expenseSplitRepository.sumTotalOwedByUser(groupId, userB_Id)).thenReturn(new BigDecimal("40.00"));
        lenient().when(expenseSplitRepository.sumTotalOwedByUser(groupId, userC_Id)).thenReturn(new BigDecimal("60.00"));

        List<MemberBalanceDTO> balances = balanceService.getGroupBalances(userId, groupId);

        assertEquals(3, balances.size());

        MemberBalanceDTO aliceBalance = balances.stream().filter(b -> b.userId().equals(userId)).findFirst().get();
        assertEquals(new BigDecimal("100.00"), aliceBalance.netBalance());

        MemberBalanceDTO bobBalance = balances.stream().filter(b -> b.userId().equals(userB_Id)).findFirst().get();
        assertEquals(new BigDecimal("-40.00"), bobBalance.netBalance());
    }

    @Test
    @DisplayName("Sad Path (Segurança): Lança erro se usuário tentar ver saldo de grupo que não pertence")
    void shouldThrowExceptionWhenGettingBalancesForForeignGroup() {
        Mockito.when(groupMemberRepository.existsByUserIdAndGroupId(userId, groupId)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> balanceService.getGroupBalances(userId, groupId));
        assertEquals("Acesso negado: Você não faz parte deste grupo.", ex.getMessage());
    }

    // ==========================================
    // 3. FUNCTION: GET SUGGESTED PAYMENTS (O Algoritmo)
    // ==========================================
    @Test
    @DisplayName("Happy Path Complexo: Algoritmo guloso divide 2 devedores para 1 credor")
    void shouldSuggestCorrectPaymentsForMultipleDebtors() {
        lenient().when(expenseRepository.sumTotalPaidByUser(groupId, userId)).thenReturn(new BigDecimal("100.00"));
        lenient().when(expenseSplitRepository.sumTotalOwedByUser(groupId, userB_Id)).thenReturn(new BigDecimal("40.00"));
        lenient().when(expenseSplitRepository.sumTotalOwedByUser(groupId, userC_Id)).thenReturn(new BigDecimal("60.00"));

        List<SuggestedPaymentResponse> suggestions = balanceService.getSuggestedPayments(userId, groupId);

        assertEquals(2, suggestions.size());

        assertTrue(suggestions.stream().anyMatch(s -> s.amount().compareTo(new BigDecimal("40.00")) == 0));
        assertTrue(suggestions.stream().anyMatch(s -> s.amount().compareTo(new BigDecimal("60.00")) == 0));

        assertTrue(suggestions.stream().allMatch(s -> s.toUserId().equals(userId)));
    }

    @Test
    @DisplayName("Edge Case: Grupo onde todos estão quites retorna lista vazia")
    void shouldReturnEmptySuggestionsWhenEveryoneIsSettled() {
        List<SuggestedPaymentResponse> suggestions = balanceService.getSuggestedPayments(userId, groupId);
        assertTrue(suggestions.isEmpty());
    }

    // ==========================================
    // 4. FUNCTION: GET INDIVIDUAL ALL BALANCE
    // ==========================================
    @Test
    @DisplayName("Happy Path: Soma saldos de múltiplos grupos corretamente")
    void shouldSumBalancesAcrossMultipleGroups() {
        Group group2 = new Group(); group2.setId(2L); group2.setTitle("Faculdade");

        GroupMember m1 = new GroupMember(); m1.setGroup(group);
        GroupMember m2 = new GroupMember(); m2.setGroup(group2);

        Mockito.when(groupMemberRepository.findAllByUserIdWithGroups(userId)).thenReturn(List.of(m1, m2));

        lenient().when(expenseRepository.sumTotalPaidByUser(1L, userId)).thenReturn(new BigDecimal("50.00"));
        lenient().when(expenseSplitRepository.sumTotalOwedByUser(2L, userId)).thenReturn(new BigDecimal("20.00"));

        IndividualBalanceDTO result = balanceService.getIndividualAllBalance(userId);

        assertEquals(new BigDecimal("30.00"), result.totalAmount());
        assertEquals(2, result.groupBalanceDTOS().size());
    }

    // ==========================================
    // 5. FUNCTION: GET BALANCE STATUS
    // ==========================================
    @Test
    @DisplayName("Happy Path: Verifica os 3 status possíveis (CREDITOR, DEBTOR, SETTLED)")
    void shouldReturnCorrectBalanceStatus() {
        Mockito.when(expenseRepository.sumTotalPaidByUser(groupId, userId)).thenReturn(new BigDecimal("10.00"));
        assertEquals(BalanceStatus.CREDITOR, balanceService.getBalanceStatus(userId, groupId));

        Mockito.reset(expenseRepository);
        Mockito.when(expenseSplitRepository.sumTotalOwedByUser(groupId, userId)).thenReturn(new BigDecimal("10.00"));
        assertEquals(BalanceStatus.DEBTOR, balanceService.getBalanceStatus(userId, groupId));

        Mockito.reset(expenseSplitRepository);
        assertEquals(BalanceStatus.SETTLED, balanceService.getBalanceStatus(userId, groupId));
    }

    // ==========================================
    // 6. FUNCTION: GET GROUP HISTORY
    // ==========================================
    @Test
    @DisplayName("Edge Case: Junta Despesas e Pagamentos e ordena da mais recente para a mais antiga")
    void shouldReturnSortedGroupHistory() {
        // ARRANGE
        Users payer = new Users();
        payer.setId(userId);
        payer.setName("Tester");

        Users receiver = new Users();
        receiver.setId(userB_Id);
        receiver.setName("Receiver");

        Expense expense = new Expense();
        expense.setId(10L);
        expense.setType(ExpenseType.MERCADO);
        expense.setDescription("Atacadão");
        expense.setTotalAmount(new BigDecimal("150.00"));
        expense.setPaidBy(payer);
        expense.setDate(java.time.Instant.now());

        Settlement settlement = new Settlement();
        settlement.setId(20L);
        settlement.setAmount(new BigDecimal("50.00"));
        settlement.setPayer(payer);
        settlement.setReceiver(receiver);
        settlement.setDate(java.time.Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS));

        Mockito.when(expenseRepository.findByGroupId(groupId)).thenReturn(List.of(expense));
        Mockito.when(settlementRepository.findByGroupId(groupId)).thenReturn(List.of(settlement));

        List<GroupActivityDTO> history = balanceService.getGroupHistory(userId, groupId);

        assertEquals(2, history.size());
        assertEquals(ActivityType.EXPENSE, history.get(0).activityType());
        assertEquals("Atacadão", history.get(0).title());

        assertEquals(ActivityType.SETTLEMENT, history.get(1).activityType());
        assertEquals("Pagamento de Tester para Receiver", history.get(1).title());
    }
}
