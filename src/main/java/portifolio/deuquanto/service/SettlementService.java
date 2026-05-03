package portifolio.deuquanto.service;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portifolio.deuquanto.dto.request.SettlementPatchRequest;
import portifolio.deuquanto.dto.request.SettlementRequest;
import portifolio.deuquanto.entity.Group;
import portifolio.deuquanto.entity.Settlement;
import portifolio.deuquanto.entity.Users;
import portifolio.deuquanto.entity.enums.BalanceStatus;
import portifolio.deuquanto.exception.BusinessException;
import portifolio.deuquanto.repository.GroupRepository;
import portifolio.deuquanto.repository.SettlementRepository;
import portifolio.deuquanto.repository.UserRepository;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class SettlementService {

    private static final Logger log = LoggerFactory.getLogger(SettlementService.class);

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
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado"));
        Users receiver = userRepository.findById(request.receiverId())
                .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado"));
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new EntityNotFoundException("Grupo nao encontrado"));


        BalanceStatus payerStatus = balanceService.getBalanceStatus(payer.getId(), group.getId());
        BalanceStatus receiverStatus = balanceService.getBalanceStatus(receiver.getId(), group.getId());

        if (payerStatus != BalanceStatus.DEBTOR) {
            throw new BusinessException("Operação inválida: O pagador não possui dívidas neste grupo.");
        }

        if (receiverStatus != BalanceStatus.CREDITOR) {
            throw new BusinessException("Operação inválida: O recebedor não possui saldo a receber neste grupo.");
        }

        BigDecimal currentDebt = balanceService.getIndividualBalance(group.getId(), payer.getId()).abs();
        if (request.amount().compareTo(currentDebt) > 0) {
            throw new BusinessException("Operação inválida: O valor do pagamento é maior do que a dívida atual.");
        }

        Settlement settlement = new Settlement();
        settlement.setAmount(request.amount());
        settlement.setPayer(payer);
        settlement.setReceiver(receiver);
        settlement.setGroup(group);

        settlementRepository.save(settlement);
        log.info("Settlement {} criada com sucesso", settlement.getId());

    }

    @Transactional
    public void patchSettlement(UUID userId, Long groupId, Long settlementId, SettlementPatchRequest request){
        groupService.validateUserIsMember(userId, groupId);

        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new EntityNotFoundException("Acerto financeiro não encontrado."));

        if (!settlement.getGroup().getId().equals(groupId)) {
            throw new BusinessException("Este acerto não pertence ao grupo informado.");
        }
        if (!settlement.getPayer().getId().equals(userId) &&
                !settlement.getReceiver().getId().equals(userId)) {
            throw new BusinessException("Apenas os envolvidos neste acerto podem editá-lo.");
        }

        if (request.amount().compareTo(settlement.getAmount()) != 0) {
            BigDecimal currentDebt = balanceService.getIndividualBalance(groupId, userId).abs();
            if (request.amount().compareTo(currentDebt) > 0) {
                throw new BusinessException("Operação inválida: O valor do pagamento é maior do que a dívida atual.");
            }
            settlement.setAmount(request.amount());
        }

        settlementRepository.save(settlement);
        log.info("Dados do settlement {} atualizados", settlement.getId());
    }

    public void deleteSettlement(UUID userId, Long groupId, Long settlementId){
        log.info("Iniciando exclusão da Settlement {}", settlementId);
        groupService.validateUserIsMember(userId, groupId);

        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new EntityNotFoundException("Acerto financeiro não encontrado."));

        if (!settlement.getPayer().getId().equals(userId) &&
                !settlement.getReceiver().getId().equals(userId)) {
            throw new BusinessException("Apenas os envolvidos neste acerto podem apagá-lo.");
        }

        if (!settlement.getGroup().getId().equals(groupId)) {
            throw new BusinessException("Este acerto não pertence ao grupo informado.");
        }

        settlementRepository.delete(settlement);
        log.info("Settlement {} excluida", settlement.getId());
    }

}
