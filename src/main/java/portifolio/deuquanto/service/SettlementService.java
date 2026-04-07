package portifolio.deuquanto.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portifolio.deuquanto.dto.request.SettlementRequest;
import portifolio.deuquanto.entity.Group;
import portifolio.deuquanto.entity.Settlement;
import portifolio.deuquanto.entity.Users;
import portifolio.deuquanto.entity.enums.BalanceStatus;
import portifolio.deuquanto.repository.GroupRepository;
import portifolio.deuquanto.repository.SettlementRepository;
import portifolio.deuquanto.repository.UserRepository;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class SettlementService {

    private final UserRepository userRepository;
    private final SettlementRepository settlementRepository;
    private final GroupRepository groupRepository;
    private final GroupService groupService;
    private final BalanceService balanceService;

    public SettlementService(UserRepository userRepository, SettlementRepository settlementRepository, GroupRepository groupRepository, GroupService groupService, BalanceService balanceService) {
        this.userRepository = userRepository;
        this.settlementRepository = settlementRepository;
        this.groupRepository = groupRepository;
        this.groupService = groupService;
        this.balanceService = balanceService;
    }

    @Transactional
    public void createSettlement(UUID loggedUserId, Long groupId, SettlementRequest request){
        groupService.validateUserIsMember(loggedUserId, groupId);
        groupService.validateUserIsMember(request.receiverId(), groupId);

        Users payer = userRepository.findById(loggedUserId)
                .orElseThrow(() -> new RuntimeException("Usuario nao encontrado"));
        Users receiver = userRepository.findById(request.receiverId())
                .orElseThrow(() -> new RuntimeException("Usuario nao encontrado"));
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Grupo nao encontrado"));


        //2 - VERIFICAR SE PAGADOR ESTÁ EM DEBITO E SE O RECEBEDOR ESTÁ COM SALDO
        BalanceStatus payerStatus = balanceService.getBalanceStatus(payer.getId(), group.getId());
        BalanceStatus receiverStatus = balanceService.getBalanceStatus(receiver.getId(), group.getId());

        if (payerStatus != BalanceStatus.DEBTOR) {
            throw new RuntimeException("Operação inválida: O pagador não possui dívidas neste grupo.");
        }

        if (receiverStatus != BalanceStatus.CREDITOR) {
            throw new RuntimeException("Operação inválida: O recebedor não possui saldo a receber neste grupo.");
        }

        //3 - VERIFICAR LIMITE DE VALOR PARA PAGAMENTO
        BigDecimal currentDebt = balanceService.getIndividualBalance(group.getId(), payer.getId()).abs();
        if (request.amount().compareTo(currentDebt) > 0) {
            throw new RuntimeException("Operação inválida: O valor do pagamento é maior do que a dívida atual.");
        }


        Settlement settlement = new Settlement();
        settlement.setAmount(request.amount());
        settlement.setPayer(payer);
        settlement.setReceiver(receiver);
        settlement.setGroup(group);

        settlementRepository.save(settlement);
    }
}
