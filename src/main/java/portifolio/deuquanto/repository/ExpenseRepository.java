package portifolio.deuquanto.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import portifolio.deuquanto.entity.Expense;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("SELECT COALESCE(SUM(e.totalAmount), 0) FROM Expense e WHERE e.group.id = :groupId AND e.paidBy.id = :userId")
    BigDecimal sumTotalPaidByUser(Long groupId, UUID userId);

    List<Expense> findByGroupId(Long groupId);
}
