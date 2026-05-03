package portifolio.deuquanto.service;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portifolio.deuquanto.dto.request.ExpenseRequest;
import portifolio.deuquanto.entity.*;
import portifolio.deuquanto.entity.enums.ExpenseType;
import portifolio.deuquanto.exception.BusinessException;
import portifolio.deuquanto.repository.ExpenseRepository;
import portifolio.deuquanto.repository.GroupRepository;
import portifolio.deuquanto.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class ExpenseService {

    private static final Logger log = LoggerFactory.getLogger(ExpenseService.class);

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final ExpenseRepository expenseRepository;
    private final GroupService groupService;

    public ExpenseService(UserRepository userRepository, GroupRepository groupRepository, ExpenseRepository expenseRepository, GroupService groupService) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.expenseRepository = expenseRepository;
        this.groupService = groupService;
    }

    public void createExpense(UUID userId, Long groupId, ExpenseRequest request){
        Users creator = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado!"));

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Grupo nao encontrado"));

        if (group.isExpired()){
            throw new BusinessException("O grupo escolhido está expirado");
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
        log.info("Expense {} criada com sucesso", newExpense.getId());
    }

    @Transactional
    public void updateExpense(UUID userId, Long groupId, Long expenseId, ExpenseRequest request){
        groupService.validateUserIsMember(userId, groupId);

        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new EntityNotFoundException("Despesa nao encontrada."));

        if (!expense.getGroup().getId().equals(groupId)){
            throw new BusinessException("Esta despesa nao pertence ao grupo informado");
        }

        if (!expense.getPaidBy().getId().equals(userId)) {
            throw new BusinessException("Apenas quem pagou a conta pode editá-la.");
        }

        if (request.description() != null && !request.description().isBlank()) {
            expense.setDescription(request.description());
        }

        if (request.type() != null) {
            expense.setType(ExpenseType.valueOf(request.type()));
        }

        if (request.amount() != null && request.amount().compareTo(expense.getTotalAmount()) != 0) {
            expense.setTotalAmount(request.amount());
            expense.getSplits().clear();

            List<GroupMember> members = groupService.getAllMembersGroups(userId);
            BigDecimal amountPerPerson = request.amount().divide(
                    new BigDecimal(members.size()),
                    2,
                    RoundingMode.HALF_UP
            );

            for (GroupMember member : members) {
                ExpenseSplit split = new ExpenseSplit();
                split.setExpense(expense);
                split.setUser(member.getUser());
                split.setAmountOwed(amountPerPerson);

                expense.getSplits().add(split);
            }
        }
        expenseRepository.save(expense);
        log.info("Dados da Expense {} atualizados", expense.getId());
    }

    public void deleteExpense(UUID userId, Long groupId, Long expenseId){
        log.info("Iniciando exclusão da expense {}", expenseId);
        groupService.validateUserIsMember(userId, groupId);

        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new EntityNotFoundException("Despesa não encontrada."));

        if (!expense.getGroup().getId().equals(groupId)) {
            throw new BusinessException("Esta despesa não pertence ao grupo informado.");
        }

        if (!expense.getPaidBy().getId().equals(userId)) {
            throw new BusinessException("Apenas o usuário que registrou a despesa pode apagá-la.");
        }

        expenseRepository.delete(expense);
        log.info("Expense {} deletada com sucesso", expense.getId());
    }

}
