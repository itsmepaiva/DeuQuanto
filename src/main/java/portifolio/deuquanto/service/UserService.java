package portifolio.deuquanto.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import portifolio.deuquanto.dto.request.RegisterUserRequest;
import portifolio.deuquanto.dto.response.RegisterUserResponse;
import portifolio.deuquanto.entity.Users;
import portifolio.deuquanto.repository.UserRepository;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findUserByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario não encontrado com o email: " + username));
    }




    public RegisterUserResponse register(RegisterUserRequest request){
        Users newUser = new Users();
        newUser.setPassword(passwordEncoder.encode(request.password()));
        newUser.setName(request.name());
        newUser.setEmail(request.email());
        newUser.setPixKey(request.pixKey());

        userRepository.save(newUser);
        return new RegisterUserResponse(newUser.getName(), newUser.getEmail());
    }
}
