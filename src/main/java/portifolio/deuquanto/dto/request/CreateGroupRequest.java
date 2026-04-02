package portifolio.deuquanto.dto.request;


import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateGroupRequest(@NotBlank(message = "O titulo do grupo é obrigatório") String title,
                                 String description,
                                 @NotNull(message = "O prazo de expiração do grupo é obrigatório")
                                 @Future(message = "A data de exppiração deve ser uma data no futuro") LocalDate expiresAt) {
}
