package portifolio.deuquanto.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

public record SettlementRequest(
        UUID receiverId,
        BigDecimal amount
) {
}
