package portifolio.deuquanto.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import portifolio.deuquanto.entity.Settlement;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    @Query("SELECT COALESCE(SUM(s.amount), 0) FROM Settlement s WHERE s.group.id = :groupId AND s.payer.id = :userId")
    BigDecimal sumTotalSentByUser(Long groupId, UUID userId);

    @Query("SELECT COALESCE(SUM(s.amount), 0) FROM Settlement s WHERE s.group.id = :groupId AND s.receiver.id = :userId")
    BigDecimal sumTotalReceivedByUser(Long groupId, UUID userId);
}
