package portifolio.deuquanto.dto;

import java.math.BigDecimal;
import java.util.List;

public record IndividualBalanceDTO(
        BigDecimal totalAmount,
        List<IndividualGroupBalanceDTO> groupBalanceDTOS
) {
}
