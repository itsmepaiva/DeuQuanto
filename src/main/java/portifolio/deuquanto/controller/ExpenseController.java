package portifolio.deuquanto.controller;

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
    public ResponseEntity<Void> newExpense(@AuthenticationPrincipal JWTUserData loggedUser,
                                           @PathVariable Long groupId,
                                           @RequestBody ExpenseRequest request){

        expenseService.createExpense(loggedUser.userId(), groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
