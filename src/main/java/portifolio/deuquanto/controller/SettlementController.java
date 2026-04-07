package portifolio.deuquanto.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import portifolio.deuquanto.dto.JWTUserData;
import portifolio.deuquanto.dto.request.SettlementRequest;
import portifolio.deuquanto.service.SettlementService;

@RestController
@RequestMapping("/api/v1/settlement")
public class SettlementController {

    @Autowired
    private SettlementService settlementService;

    @PostMapping("/{groupId}")
    public ResponseEntity<Void> addSettlement(@AuthenticationPrincipal JWTUserData loggedUser,
                                              @PathVariable Long groupId,
                                              @RequestBody SettlementRequest request
                                              ){
        settlementService.createSettlement(loggedUser.userId(), groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
