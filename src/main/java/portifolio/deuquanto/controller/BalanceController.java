package portifolio.deuquanto.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import portifolio.deuquanto.dto.JWTUserData;
import portifolio.deuquanto.dto.MemberBalanceDTO;
import portifolio.deuquanto.dto.SuggestedPaymentDTO;
import portifolio.deuquanto.service.BalanceService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/balance")
public class BalanceController {

    @Autowired
    private BalanceService balanceService;

    @GetMapping("/group/{groupId}/")
    public ResponseEntity<List<MemberBalanceDTO>> getGroupBalance(
            @AuthenticationPrincipal JWTUserData loggedUser,
            @PathVariable Long groupId){


        List<MemberBalanceDTO> balances = balanceService.getGroupBalances(loggedUser.userId(), groupId);
        return ResponseEntity.ok(balances);
    }

    @GetMapping("/group/{groupId}//sugestion/")
    public ResponseEntity<List<SuggestedPaymentDTO>> getSuggestedPayment(
            @AuthenticationPrincipal JWTUserData loggedUser,
            @PathVariable Long groupId){
        List<SuggestedPaymentDTO> suggestion = balanceService.getSuggestedPayments(loggedUser.userId(), groupId);
        return ResponseEntity.ok(suggestion);
    }

}
