package portifolio.deuquanto.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(
        @NotBlank(message = "A seleção do tipo nao pode estar vazia") String type,
        @NotBlank(message = "A descriçao nao pode estar vazia")String description,
        @NotNull(message = "O valor nao pode estar vazio")
        @Positive(message = "O valor nao pode ser negativo") BigDecimal amount,
        @NotNull(message = "A data nao pode estar vazia")LocalDate date
) {
}
