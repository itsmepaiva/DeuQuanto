package portifolio.deuquanto.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.util.UUID;

public record MemberBalanceDTO(
        UUID userId,
        String name,
        BigDecimal netBalance,

        @JsonIgnore
        String pixKey
) {
}
