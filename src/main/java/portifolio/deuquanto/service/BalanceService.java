package portifolio.deuquanto.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portifolio.deuquanto.dto.GroupActivityDTO;
import portifolio.deuquanto.dto.IndividualBalanceDTO;
import portifolio.deuquanto.dto.IndividualGroupBalanceDTO;
import portifolio.deuquanto.dto.MemberBalanceDTO;
import portifolio.deuquanto.dto.response.SuggestedPaymentResponse;
import portifolio.deuquanto.entity.*;
import portifolio.deuquanto.entity.enums.ActivityType;
import portifolio.deuquanto.entity.enums.BalanceStatus;
import portifolio.deuquanto.repository.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
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
                    balance,
                    null
            ));
        }

        return balances;
    }

    @Transactional(readOnly = true)
    public List<SuggestedPaymentResponse> getSuggestedPayments(UUID userId, Long groupId) {

        List<MemberBalanceDTO> balances = getGroupBalances(userId, groupId);

        List<MutableBalance> debtors = new ArrayList<>();
        List<MutableBalance> creditors = new ArrayList<>();

        for (MemberBalanceDTO b : balances) {
            if (b.netBalance().compareTo(ZERO) < 0) {
                debtors.add(new MutableBalance(b.userId(), b.name(), b.netBalance().abs(), b.pixKey()));
            } else if (b.netBalance().compareTo(ZERO) > 0) {
                creditors.add(new MutableBalance(b.userId(), b.name(), b.netBalance(), b.pixKey()));
            }
        }

        List<SuggestedPaymentResponse> suggestedPayments = new ArrayList<>();

        int i = 0; // Índice da fila de Devedores
        int j = 0; // Índice da fila de Credores

        while (i < debtors.size() && j < creditors.size()) {
            MutableBalance debtor = debtors.get(i);
            MutableBalance creditor = creditors.get(j);

            BigDecimal amountToTransfer = debtor.amount.min(creditor.amount);
            suggestedPayments.add(new SuggestedPaymentResponse(
                    debtor.userId, debtor.name,
                    creditor.userId, creditor.name,
                    amountToTransfer,
                    creditor.pixKey
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

    public IndividualBalanceDTO getIndividualAllBalance(UUID userID){
        List<GroupMember> membership = groupService.getAllMembersGroups(userID);

        List<IndividualGroupBalanceDTO> groupBalance = new ArrayList<>();
        BigDecimal totalBalance = ZERO;

        for (GroupMember g : membership){
            BigDecimal balance = getIndividualBalance(g.getGroup().getId(), userID);
            groupBalance.add(new IndividualGroupBalanceDTO(
                    g.getGroup().getTitle(),
                    balance
            ));
            if(balance != null){
                totalBalance = totalBalance.add(balance);
            }
        }
        return new IndividualBalanceDTO(totalBalance, groupBalance);
    }


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

    public List<GroupActivityDTO> getGroupHistory(UUID loggedUserId, Long groupId){
        groupService.validateUserIsMember(loggedUserId, groupId);

        List<GroupActivityDTO> history = new ArrayList<>();

        List<Expense> expenses = expenseRepository.findByGroupId(groupId);
        for (Expense e : expenses){
            history.add(new GroupActivityDTO(
                    e.getId(),
                    ActivityType.EXPENSE,
                    e.getType(),
                    e.getDescription(),
                    e.getTotalAmount(),
                    e.getDate(),
                    e.getPaidBy().getName()
            ));
        }

        List<Settlement> settlements = settlementRepository.findByGroupId(groupId);
        for (Settlement s : settlements){
            String paymentTitle = "Pagamento de " + s.getPayer().getName() + " para " + s.getReceiver();
            history.add(new GroupActivityDTO(
                    s.getId(),
                    ActivityType.SETTLEMENT,
                    null,
                    paymentTitle,
                    s.getAmount(),
                    s.getDate(),
                    s.getPayer().getName()
            ));
        }

        history.sort(Comparator.comparing(GroupActivityDTO::date).reversed());
        return history;
    }



    private static class MutableBalance {
        UUID userId;
        String name;
        BigDecimal amount;
        String pixKey;

        MutableBalance(UUID userId, String name, BigDecimal amount, String pixKey) {
            this.userId = userId;
            this.name = name;
            this.amount = amount;
            this.pixKey = pixKey;
        }
    }
}
