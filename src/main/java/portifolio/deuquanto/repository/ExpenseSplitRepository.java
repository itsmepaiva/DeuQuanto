package portifolio.deuquanto.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import portifolio.deuquanto.entity.ExpenseSplit;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, Long> {

    @Query("SELECT COALESCE(SUM(es.amountOwed), 0) FROM ExpenseSplit es WHERE es.expense.group.id = :groupId AND es.user.id = :userId")
    BigDecimal sumTotalOwedByUser(Long groupId, UUID userId);
}
