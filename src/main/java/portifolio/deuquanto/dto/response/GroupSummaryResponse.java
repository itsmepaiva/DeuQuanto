package portifolio.deuquanto.dto.response;

import java.math.BigDecimal;

public record GroupSummaryResponse(
        Long id,
        String title,
        int memberCount,
        boolean isExpired,
        BigDecimal balance
) {
}
