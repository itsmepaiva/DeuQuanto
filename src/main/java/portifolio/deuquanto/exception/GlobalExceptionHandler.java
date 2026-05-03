package portifolio.deuquanto.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import portifolio.deuquanto.service.EmailService;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorMessage> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request){
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        log.warn("Falha de validação de dados em [{}]: {}", request.getRequestURI(), errors);
        return buildErrorMessage(HttpStatus.BAD_REQUEST, "Erro de Validação nos Dados", errors, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorMessage> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request){
        List<String> errors = ex.getConstraintViolations().stream()
                .map(error -> error.getPropertyPath() + ":" + error.getMessage())
                .toList();

        log.warn("Falha de validação de parâmetros em [{}]: {}", request.getRequestURI(), errors);
        return buildErrorMessage(HttpStatus.BAD_REQUEST, "Erro de validação nos Paramentros", errors, request);
    }

    @ExceptionHandler(BusinessException.class) // Mudamos aqui!
    public ResponseEntity<ErrorMessage> handleBusinessException(BusinessException ex, HttpServletRequest request){

        log.warn("Regra de negócio violada em [{}]: {}", request.getRequestURI(), ex.getMessage());

        return buildErrorMessage(HttpStatus.UNPROCESSABLE_CONTENT, "Ação não permitida", List.of(ex.getMessage()), request);
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessage> handleGenericException(Exception ex, HttpServletRequest request){
        log.error("ERRO CRÍTICO NÃO TRATADO em [{}]: {}", request.getRequestURI(), ex.getMessage(), ex);

        List<String> messages = List.of("Ocorreu um erro interno no servidor. Tente novamente mais tarde.");
        return buildErrorMessage(HttpStatus.INTERNAL_SERVER_ERROR, "Falha Interna do Sistema", messages, request);
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
