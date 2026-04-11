package portifolio.deuquanto.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.aspectj.apache.bcel.generic.LocalVariableGen;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import portifolio.deuquanto.dto.JWTUserData;
import portifolio.deuquanto.dto.request.SettlementPatchRequest;
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
                                              @Valid @RequestBody SettlementRequest request){
        settlementService.createSettlement(loggedUser.userId(), groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/{groupId}/{settlementId}")
    @Operation(summary = "Edita o valor de um acerto", description = "Altera o valor de um pagamento já registrado.")
    public ResponseEntity<Void> patchSettlement(@AuthenticationPrincipal JWTUserData loggedUser,
                                                @PathVariable Long groupId, @PathVariable Long settlementId,
                                                @Valid @RequestBody SettlementPatchRequest request){
        settlementService.patchSettlement(loggedUser.userId(), groupId, settlementId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{groupId}/{settlementId}")
    @Operation(summary = "Apaga um acerto (Hard Delete)", description = "Remove o pagamento e desfaz a quitação da dívida automaticamente.")
    public ResponseEntity<Void> deleteSettlement(@AuthenticationPrincipal JWTUserData loggedUser,
                                                 @PathVariable Long groupId, @PathVariable Long settlementId){
        settlementService.deleteSettlement(loggedUser.userId(), groupId, settlementId);
        return ResponseEntity.ok().build();
    }

}
