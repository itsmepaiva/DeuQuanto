package portifolio.deuquanto.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portifolio.deuquanto.dto.MemberBalanceDTO;
import portifolio.deuquanto.dto.SuggestedPaymentDTO;
import portifolio.deuquanto.entity.Group;
import portifolio.deuquanto.entity.GroupMember;
import portifolio.deuquanto.entity.Users;
import portifolio.deuquanto.entity.enums.BalanceStatus;
import portifolio.deuquanto.repository.ExpenseRepository;
import portifolio.deuquanto.repository.ExpenseSplitRepository;
import portifolio.deuquanto.repository.GroupRepository;
import portifolio.deuquanto.repository.SettlementRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.math.BigDecimal.ZERO;


@Service
public class BalanceService {
    private final GroupRepository groupRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final ExpenseRepository expenseRepository;
    private final GroupService groupService;
    private final SettlementRepository settlementRepository;

    public BalanceService(GroupRepository groupRepository, ExpenseSplitRepository expenseSplitRepository, ExpenseRepository expenseRepository, GroupService groupService, SettlementRepository settlementRepository) {
        this.groupRepository = groupRepository;
        this.expenseSplitRepository = expenseSplitRepository;
        this.expenseRepository = expenseRepository;
        this.groupService = groupService;
        this.settlementRepository = settlementRepository;
    }

    @Transactional(readOnly = true)
    public List<MemberBalanceDTO> getGroupBalances(UUID userId, Long groupId) {
        groupService.validateUserIsMember(userId, groupId);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Grupo não encontrado"));

        List<MemberBalanceDTO> balances = new ArrayList<>();

        for (GroupMember member : group.getGroupMembers()) {
            Users user = member.getUser();

            BigDecimal balance = getIndividualBalance(group.getId(), user.getId());

            balances.add(new MemberBalanceDTO(
                    user.getId(),
                    user.getName(),
                    balance
            ));
        }

        return balances;
    }

    @Transactional(readOnly = true)
    public List<SuggestedPaymentDTO> getSuggestedPayments(UUID userId, Long groupId) {

        List<MemberBalanceDTO> balances = getGroupBalances(userId, groupId);

        List<MutableBalance> debtors = new ArrayList<>();
        List<MutableBalance> creditors = new ArrayList<>();

        for (MemberBalanceDTO b : balances) {
            if (b.netBalance().compareTo(ZERO) < 0) {
                debtors.add(new MutableBalance(b.userId(), b.name(), b.netBalance().abs()));
            } else if (b.netBalance().compareTo(ZERO) > 0) {
                creditors.add(new MutableBalance(b.userId(), b.name(), b.netBalance()));
            }
        }

        List<SuggestedPaymentDTO> suggestedPayments = new ArrayList<>();

        int i = 0; // Índice da fila de Devedores
        int j = 0; // Índice da fila de Credores

        while (i < debtors.size() && j < creditors.size()) {
            MutableBalance debtor = debtors.get(i);
            MutableBalance creditor = creditors.get(j);

            BigDecimal amountToTransfer = debtor.amount.min(creditor.amount);
            suggestedPayments.add(new SuggestedPaymentDTO(
                    debtor.userId, debtor.name,
                    creditor.userId, creditor.name,
                    amountToTransfer
            ));

            debtor.amount = debtor.amount.subtract(amountToTransfer);
            creditor.amount = creditor.amount.subtract(amountToTransfer);

            if (debtor.amount.compareTo(ZERO) == 0) {
                i++;
            }
            if (creditor.amount.compareTo(ZERO) == 0) {
                j++;
            }
        }

        return suggestedPayments;
    }

    public BigDecimal getIndividualBalance(Long groupId, UUID userId){
        BigDecimal expensesPaid = expenseRepository.sumTotalPaidByUser(groupId, userId);
        BigDecimal expensesOwed = expenseSplitRepository.sumTotalOwedByUser(groupId, userId);

        BigDecimal paymentSent = settlementRepository.sumTotalSentByUser(groupId, userId);
        BigDecimal paymentReceived = settlementRepository.sumTotalReceivedByUser(groupId, userId);

        BigDecimal totalCredits = expensesPaid.add(paymentSent);
        BigDecimal totalDebits = expensesOwed.add(paymentReceived);

        return totalCredits.subtract(totalDebits);
    };

    public BalanceStatus getBalanceStatus(UUID userId, Long groupId){
        BigDecimal balance = getIndividualBalance(groupId, userId);
        int sign = balance.signum();
        if (sign > 0){
            return BalanceStatus.CREDITOR;
        } else if (sign < 0) {
            return BalanceStatus.DEBTOR;
        } else {
            return BalanceStatus.SETTLED;
        }
    };


    private static class MutableBalance {
        UUID userId;
        String name;
        BigDecimal amount;

        MutableBalance(UUID userId, String name, BigDecimal amount) {
            this.userId = userId;
            this.name = name;
            this.amount = amount;
        }
    }
}
