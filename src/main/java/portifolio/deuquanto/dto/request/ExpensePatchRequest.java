package portifolio.deuquanto.dto.request;

import java.math.BigDecimal;

public record ExpensePatchRequest(
        String type,
        String description,
        BigDecimal amount
) {
}
