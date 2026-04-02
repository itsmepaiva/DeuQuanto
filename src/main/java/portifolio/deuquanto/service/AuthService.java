package portifolio.deuquanto.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import portifolio.deuquanto.dto.request.LoginRequest;
import portifolio.deuquanto.dto.response.LoginResponse;
import portifolio.deuquanto.entity.Users;
import portifolio.deuquanto.security.TokenConfig;

import java.util.Objects;

@Service
public class AuthService {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenConfig tokenConfig;

    public LoginResponse login(LoginRequest loginRequest){
        UsernamePasswordAuthenticationToken userPassAuth = new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password());
        Authentication authentication = authenticationManager.authenticate(userPassAuth);

        Users user = (Users) authentication.getPrincipal();
        String token = tokenConfig.generateToken(Objects.requireNonNull(user));
        return new LoginResponse(token);
    }
}
