package portifolio.deuquanto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import portifolio.deuquanto.dto.request.SettlementPatchRequest;
import portifolio.deuquanto.dto.request.SettlementRequest;
import portifolio.deuquanto.entity.Group;
import portifolio.deuquanto.entity.Settlement;
import portifolio.deuquanto.entity.Users;
import portifolio.deuquanto.entity.enums.BalanceStatus;
import portifolio.deuquanto.repository.GroupRepository;
import portifolio.deuquanto.repository.SettlementRepository;
import portifolio.deuquanto.repository.UserRepository;
import portifolio.deuquanto.service.BalanceService;
import portifolio.deuquanto.service.GroupService;
import portifolio.deuquanto.service.SettlementService;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private SettlementRepository settlementRepository;
    @Mock private GroupRepository groupRepository;
    @Mock private GroupService groupService;
    @Mock private BalanceService balanceService;

    @InjectMocks
    private SettlementService settlementService;

    private UUID payerId;
    private UUID receiverId;
    private UUID intruderId;
    private Long groupId;
    private Long settlementId;

    private Users payer;
    private Users receiver;
    private Group group;
    private Settlement existingSettlement;

    @BeforeEach
    void setUp() {
        payerId = UUID.randomUUID();
        receiverId = UUID.randomUUID();
        intruderId = UUID.randomUUID();
        groupId = 1L;
        settlementId = 100L;

        payer = new Users();
        payer.setId(payerId);
        payer.setName("Pagador");

        receiver = new Users();
        receiver.setId(receiverId);
        receiver.setName("Recebedor");

        group = new Group();
        group.setId(groupId);
        group.setTitle("República");

        existingSettlement = new Settlement();
        existingSettlement.setId(settlementId);
        existingSettlement.setAmount(new BigDecimal("50.00"));
        existingSettlement.setPayer(payer);
        existingSettlement.setReceiver(receiver);
        existingSettlement.setGroup(group);

        lenient().when(userRepository.findById(payerId)).thenReturn(Optional.of(payer));
        lenient().when(userRepository.findById(receiverId)).thenReturn(Optional.of(receiver));
        lenient().when(groupRepository.findById(groupId)).thenReturn(Optional.of(group));
        lenient().when(settlementRepository.findById(settlementId)).thenReturn(Optional.of(existingSettlement));

        lenient().doNothing().when(groupService).validateUserIsMember(any(UUID.class), anyLong());

        lenient().when(balanceService.getBalanceStatus(payerId, groupId)).thenReturn(BalanceStatus.DEBTOR);
        lenient().when(balanceService.getBalanceStatus(receiverId, groupId)).thenReturn(BalanceStatus.CREDITOR);

        lenient().when(balanceService.getIndividualBalance(groupId, payerId)).thenReturn(new BigDecimal("-100.00"));
    }

    // ==========================================
    // 1. FUNCTION: CREATE SETTLEMENT
    // ==========================================
    @Test
    @DisplayName("Happy Path: Cria settlement com todos os requisitos corretos")
    void shouldCreateSettlementSuccessfully() {
        SettlementRequest request = new SettlementRequest(receiverId, new BigDecimal("50.00"));

        assertDoesNotThrow(() -> settlementService.createSettlement(payerId, groupId, request));
        Mockito.verify(settlementRepository).save(any(Settlement.class));
    }

    @Test
    @DisplayName("Sad Path: Pagador não tem débito a ser pago")
    void shouldThrowExceptionWhenPayerIsNotDebtor() {
        lenient().when(balanceService.getBalanceStatus(payerId, groupId)).thenReturn(BalanceStatus.SETTLED);
        SettlementRequest request = new SettlementRequest(receiverId, new BigDecimal("50.00"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> settlementService.createSettlement(payerId, groupId, request));
        assertEquals("Operação inválida: O pagador não possui dívidas neste grupo.", ex.getMessage());
    }

    @Test
    @DisplayName("Sad Path: Recebedor não tem saldo a receber")
    void shouldThrowExceptionWhenReceiverIsNotCreditor() {
        lenient().when(balanceService.getBalanceStatus(receiverId, groupId)).thenReturn(BalanceStatus.DEBTOR);
        SettlementRequest request = new SettlementRequest(receiverId, new BigDecimal("50.00"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> settlementService.createSettlement(payerId, groupId, request));
        assertEquals("Operação inválida: O recebedor não possui saldo a receber neste grupo.", ex.getMessage());
    }

    @Test
    @DisplayName("Edge Case: O pagamento é maior que a dívida")
    void shouldThrowExceptionWhenPaymentIsGreaterThanDebt() {
        SettlementRequest request = new SettlementRequest(receiverId, new BigDecimal("150.00"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> settlementService.createSettlement(payerId, groupId, request));
        assertEquals("Operação inválida: O valor do pagamento é maior do que a dívida atual.", ex.getMessage());
    }

    // ==========================================
    // 2. FUNCTION: UPDATE (PATCH) SETTLEMENT
    // ==========================================
    @Test
    @DisplayName("Happy Path: Valor do Settlement é atualizado com sucesso")
    void shouldUpdateSettlementAmount() {
        SettlementPatchRequest request = new SettlementPatchRequest(new BigDecimal("80.00"));

        settlementService.patchSettlement(payerId, groupId, settlementId, request);

        assertEquals(new BigDecimal("80.00"), existingSettlement.getAmount());
        Mockito.verify(settlementRepository).save(existingSettlement);
    }

    @Test
    @DisplayName("Sad Path: Settlement não pertence ao grupo")
    void shouldThrowExceptionWhenPatchingSettlementFromDifferentGroup() {
        SettlementPatchRequest request = new SettlementPatchRequest(new BigDecimal("80.00"));
        Long wrongGroupId = 99L;

        RuntimeException ex = assertThrows(RuntimeException.class, () -> settlementService.patchSettlement(payerId, wrongGroupId, settlementId, request));
        assertEquals("Este acerto não pertence ao grupo informado.", ex.getMessage());
    }

    @Test
    @DisplayName("Sad Path: Usuário não está envolvido no acerto")
    void shouldThrowExceptionWhenUninvolvedUserTriesToPatch() {
        SettlementPatchRequest request = new SettlementPatchRequest(new BigDecimal("80.00"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> settlementService.patchSettlement(intruderId, groupId, settlementId, request));
        assertEquals("Apenas os envolvidos neste acerto podem editá-lo.", ex.getMessage());
    }

    @Test
    @DisplayName("Edge Case (Patch): O novo valor é maior que a dívida atual")
    void shouldThrowExceptionWhenPatchedAmountIsGreaterThanDebt() {
        SettlementPatchRequest request = new SettlementPatchRequest(new BigDecimal("150.00"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> settlementService.patchSettlement(payerId, groupId, settlementId, request));
        assertEquals("Operação inválida: O valor do pagamento é maior do que a dívida atual.", ex.getMessage());
    }

    // ==========================================
    // 3. FUNCTION: DELETE SETTLEMENT
    // ==========================================
    @Test
    @DisplayName("Happy Path: Deleta o acerto com sucesso")
    void shouldDeleteSettlementSuccessfully() {
        assertDoesNotThrow(() -> settlementService.deleteSettlement(payerId, groupId, settlementId));
        Mockito.verify(settlementRepository).delete(existingSettlement);
    }

    @Test
    @DisplayName("Sad Path: Acerto não pertence ao grupo ao tentar deletar")
    void shouldThrowExceptionWhenDeletingSettlementFromDifferentGroup() {
        Long wrongGroupId = 99L;

        RuntimeException ex = assertThrows(RuntimeException.class, () -> settlementService.deleteSettlement(payerId, wrongGroupId, settlementId));
        assertEquals("Este acerto não pertence ao grupo informado.", ex.getMessage());

        Mockito.verify(settlementRepository, Mockito.never()).delete(any());
    }
}
