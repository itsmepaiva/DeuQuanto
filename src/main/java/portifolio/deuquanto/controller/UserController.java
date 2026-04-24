package portifolio.deuquanto.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import portifolio.deuquanto.dto.JWTUserData;
import portifolio.deuquanto.dto.UserProfileDTO;
import portifolio.deuquanto.dto.request.LoginRequest;
import portifolio.deuquanto.dto.request.UserPatchRequest;
import portifolio.deuquanto.dto.response.LoginResponse;
import portifolio.deuquanto.dto.request.RegisterUserRequest;
import portifolio.deuquanto.dto.response.RegisterUserResponse;
import portifolio.deuquanto.service.AuthService;
import portifolio.deuquanto.service.UserService;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest){
        LoginResponse loginResponse = authService.login(loginRequest);
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterUserResponse> register(@Valid @RequestBody RegisterUserRequest request){
        RegisterUserResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatusCode.valueOf(201)).body(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Busca o perfil do usuário logado", description = "Retorna os dados do próprio usuário baseado no Token JWT.")
    public ResponseEntity<UserProfileDTO> getMyProfile(@AuthenticationPrincipal JWTUserData loggedUser) {
        UserProfileDTO profile = userService.getUserProfile(loggedUser.userId());
        return ResponseEntity.ok(profile);
    }

    @PatchMapping("/me")
    @Operation(summary = "Atualiza o perfil", description = "Edita os dados do usuário logado. Apenas os campos enviados serão alterados.")
    public ResponseEntity<Void> updateMyProfile(@AuthenticationPrincipal JWTUserData loggedUser,
                                                @Valid @RequestBody UserPatchRequest request) {
        userService.updateUserProfile(loggedUser.userId(), request);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/me")
    @Operation(summary = "Exclui a conta", description = "Apaga a conta do usuário logado permanentemente, caso ele não tenha pendências.")
    public ResponseEntity<Void> deleteMyAccount(@AuthenticationPrincipal JWTUserData loggedUser) {
        userService.deleteUserAccount(loggedUser.userId());
        return ResponseEntity.noContent().build();
    }
}
