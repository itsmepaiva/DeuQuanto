package portifolio.deuquanto.controller;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import portifolio.deuquanto.dto.LoginRequest;
import portifolio.deuquanto.dto.LoginResponse;
import portifolio.deuquanto.dto.RegisterUserRequest;
import portifolio.deuquanto.dto.RegisterUserResponse;
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
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest){
        LoginResponse loginResponse = authService.login(loginRequest);
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterUserResponse> register(@RequestBody RegisterUserRequest request){
        RegisterUserResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatusCode.valueOf(201)).body(response);
    }
}
