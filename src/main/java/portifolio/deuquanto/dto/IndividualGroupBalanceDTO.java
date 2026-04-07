package portifolio.deuquanto.dto;

import java.math.BigDecimal;

public record IndividualGroupBalanceDTO(
        String GroupTitle,
        BigDecimal amount
) {
}
