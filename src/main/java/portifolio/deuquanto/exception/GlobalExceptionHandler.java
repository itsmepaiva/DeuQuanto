package portifolio.deuquanto.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorMessage> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request){
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        return buildErrorMessage(HttpStatus.BAD_REQUEST, "Erro de Validação nos Dados", errors, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorMessage> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request){
        List<String> errors = ex.getConstraintViolations().stream()
                .map(error -> error.getPropertyPath() + ":" + error.getMessage())
                .toList();

        return buildErrorMessage(HttpStatus.BAD_REQUEST, "Erro de validação nos Paramentros", errors, request);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorMessage> handleRuntimeException(RuntimeException ex, HttpServletRequest request){
        return buildErrorMessage(HttpStatus.BAD_REQUEST, "Erro na regra de negócio", List.of(ex.getMessage()), request);
    }

    private ResponseEntity<ErrorMessage> buildErrorMessage(HttpStatus status, String errorTitle, List<String> messages, HttpServletRequest request){
        ErrorMessage errorMessage = new ErrorMessage(
                Instant.now(),
                status.value(),
                errorTitle,
                messages,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(errorMessage);
    }
}
