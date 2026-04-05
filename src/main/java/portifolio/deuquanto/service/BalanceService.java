package portifolio.deuquanto.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portifolio.deuquanto.dto.MemberBalanceDTO;
import portifolio.deuquanto.dto.SuggestedPaymentDTO;
import portifolio.deuquanto.entity.Group;
import portifolio.deuquanto.entity.GroupMember;
import portifolio.deuquanto.entity.Users;
import portifolio.deuquanto.repository.ExpenseRepository;
import portifolio.deuquanto.repository.ExpenseSplitRepository;
import portifolio.deuquanto.repository.GroupRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Service
public class BalanceService {
    private final GroupRepository groupRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final ExpenseRepository expenseRepository;
    private final GroupService groupService;

    public BalanceService(GroupRepository groupRepository, ExpenseSplitRepository expenseSplitRepository, ExpenseRepository expenseRepository, GroupService groupService) {
        this.groupRepository = groupRepository;
        this.expenseSplitRepository = expenseSplitRepository;
        this.expenseRepository = expenseRepository;
        this.groupService = groupService;
    }

    @Transactional(readOnly = true)
    public List<MemberBalanceDTO> getGroupBalances(UUID userId, Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Grupo não encontrado"));

        groupService.isGroupMember(userId, groupId);

        List<MemberBalanceDTO> balances = new ArrayList<>();

        for (GroupMember member : group.getGroupMembers()) {
            Users user = member.getUser();

            BigDecimal totalPaid = expenseRepository.sumTotalPaidByUser(group.getId(), user.getId());
            BigDecimal totalOwed = expenseSplitRepository.sumTotalOwedByUser(group.getId(), user.getId());

            BigDecimal netBalance = totalPaid.subtract(totalOwed);

            balances.add(new MemberBalanceDTO(
                    user.getId(),
                    user.getName(), // Ou o campo que você usa para o nome do usuário
                    netBalance
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
            if (b.netBalance().compareTo(BigDecimal.ZERO) < 0) {
                debtors.add(new MutableBalance(b.userId(), b.name(), b.netBalance().abs()));
            } else if (b.netBalance().compareTo(BigDecimal.ZERO) > 0) {
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

            if (debtor.amount.compareTo(BigDecimal.ZERO) == 0) {
                i++;
            }
            if (creditor.amount.compareTo(BigDecimal.ZERO) == 0) {
                j++;
            }
        }

        return suggestedPayments;
    }

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
