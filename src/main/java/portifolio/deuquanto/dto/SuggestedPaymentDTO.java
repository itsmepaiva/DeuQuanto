package portifolio.deuquanto.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SuggestedPaymentDTO(
        UUID fromUserId,
        String fromUserName,
        UUID toUserId,
        String toUserName,
        BigDecimal amount
){
}
