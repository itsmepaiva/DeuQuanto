package portifolio.deuquanto.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SettlementPatchRequest(
        @NotNull(message = "O valor nao pode estar vazio.")
        @Positive(message = "O valor do acerto deve ser maior que zero.")
        BigDecimal amount
) {
}
