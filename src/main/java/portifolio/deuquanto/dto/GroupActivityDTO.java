package portifolio.deuquanto.dto;

import portifolio.deuquanto.entity.enums.ActivityType;
import portifolio.deuquanto.entity.enums.ExpenseType;

import java.math.BigDecimal;
import java.time.Instant;

public record GroupActivityDTO(
        Long referenceId,
        ActivityType activityType,
        ExpenseType category,
        String title,
        BigDecimal amount,
        Instant date,
        String paidByName
) {
}

