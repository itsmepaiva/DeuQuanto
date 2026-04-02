package portifolio.deuquanto.exception;

import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

@Builder
public record ErrorMessage(
        Instant timestamp,
        Integer status,
        String error,
        List<String> messages,
        String pathRequest
) {
}
