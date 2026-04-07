package portifolio.deuquanto.service;

import org.springframework.stereotype.Service;
import portifolio.deuquanto.dto.request.ExpenseRequest;
import portifolio.deuquanto.entity.*;
import portifolio.deuquanto.entity.enums.ExpenseType;
import portifolio.deuquanto.repository.ExpenseRepository;
import portifolio.deuquanto.repository.GroupRepository;
import portifolio.deuquanto.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class ExpenseService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final ExpenseRepository expenseRepository;

    public ExpenseService(UserRepository userRepository, GroupRepository groupRepository, ExpenseRepository expenseRepository) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.expenseRepository = expenseRepository;
    }

    public void createExpense(UUID userId, Long groupId, ExpenseRequest request){
        Users creator = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario nao encontrado!"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Grupo nao encontrado"));

        if (group.isExpired()){
            throw new RuntimeException("O grupo escolhido está expirado");
        };

        Expense newExpense = new Expense();
        newExpense.setType(ExpenseType.valueOf(request.type()));
        newExpense.setDescription(request.description());
        newExpense.setTotalAmount(request.amount());
        newExpense.setPaidBy(creator);
        newExpense.setGroup(group);
        if(request.date() != null){
            Instant instantExpiration = request.date()
                    .atTime(0,0,1)
                    .atZone(ZoneId.of("America/Sao_Paulo"))
                    .toInstant();
            newExpense.setDate(instantExpiration);
        } else{
            newExpense.setDate(Instant.now());
        }

        var totalMembers = group.getTotalMembers();
        var groupMembers = group.getGroupMembers();

        BigDecimal splitValue = (request.amount().divide(
                BigDecimal.valueOf(totalMembers),
                2, RoundingMode.HALF_UP));

        for(GroupMember membership : groupMembers){
            ExpenseSplit newExpenseSplit = new ExpenseSplit();
            newExpenseSplit.setUser(membership.getUser());
            newExpenseSplit.setAmountOwed(splitValue);

            newExpense.addSplit(newExpenseSplit);
        }

        expenseRepository.save(newExpense);
    }



}
