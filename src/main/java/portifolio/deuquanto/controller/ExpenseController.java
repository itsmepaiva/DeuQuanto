package portifolio.deuquanto.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.tags.HtmlEscapeTag;
import portifolio.deuquanto.dto.JWTUserData;
import portifolio.deuquanto.dto.request.ExpenseRequest;
import portifolio.deuquanto.service.ExpenseService;

@RestController
@RequestMapping("/api/v1/expense")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @PostMapping("/{groupId}")
    @Operation(
            summary = "Adiciona uma despesa",
            description = "Cria uma despesa com os dados indicados e automaticamente é realizado a divisao de fatias(splits) entre os membros do grupo")
    public ResponseEntity<Void> newExpense(@AuthenticationPrincipal JWTUserData loggedUser,
                                           @PathVariable Long groupId,
                                           @RequestBody ExpenseRequest request){

        expenseService.createExpense(loggedUser.userId(), groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/{groupId}/{expenseId}")
    @Operation(
            summary = "Edita parcialmente uma despesa",
            description = "Atualiza apenas os campos enviados no JSON. Se o valor (amount) for alterado, a divisão de fatias será recalculada automaticamente.")
    public ResponseEntity<Void> patchExpense(@AuthenticationPrincipal JWTUserData loggedUser,
                                             @PathVariable Long groupId, @PathVariable Long expenseId,
                                             @Valid @RequestBody ExpenseRequest request){
        expenseService.updateExpense(loggedUser.userId(), groupId, expenseId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    @Operation(
            summary = "Deleta uma despesa",
            description = "Deleta a despesa selecionada e juntamente as fatias(splits) criadas em conjunto")
    public ResponseEntity<Void> deleteExpense(@AuthenticationPrincipal JWTUserData loggedUser,
                                              @PathVariable Long groupId, @PathVariable Long expenseId){
        expenseService.deleteExpense(loggedUser.userId(), groupId, expenseId);
        return ResponseEntity.ok().build();
    }
}
