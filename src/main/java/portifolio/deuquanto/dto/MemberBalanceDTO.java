package portifolio.deuquanto.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MemberBalanceDTO(
        UUID userId,
        String name,
        BigDecimal netBalance
) {
}
