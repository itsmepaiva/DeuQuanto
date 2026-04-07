package portifolio.deuquanto.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record SuggestedPaymentResponse(
        UUID fromUserId,
        String fromUserName,
        UUID toUserId,
        String toUserName,
        BigDecimal amount,
        String pixKey
){
}
