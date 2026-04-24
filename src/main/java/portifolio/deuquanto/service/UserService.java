package portifolio.deuquanto.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import portifolio.deuquanto.dto.UserProfileDTO;
import portifolio.deuquanto.dto.request.RegisterUserRequest;
import portifolio.deuquanto.dto.request.UserPatchRequest;
import portifolio.deuquanto.dto.response.RegisterUserResponse;
import portifolio.deuquanto.entity.Users;
import portifolio.deuquanto.repository.GroupMemberRepository;
import portifolio.deuquanto.repository.UserRepository;

import java.util.UUID;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GroupMemberRepository groupMemberRepository;


    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, GroupMemberRepository groupMemberRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.groupMemberRepository = groupMemberRepository;
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

    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfile(UUID userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        return new UserProfileDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPixKey()
        );
    }

    @Transactional
    public void updateUserProfile(UUID userId, UserPatchRequest request) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name());
        }
        if (request.pixKey() != null) {
            user.setPixKey(request.pixKey());
        }

        userRepository.save(user);
    }

    @Transactional
    public void deleteUserAccount(UUID userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        boolean isInActiveGroups = groupMemberRepository.existsByUserId(userId);

        if (isInActiveGroups) {
            throw new RuntimeException("Você não pode excluir sua conta enquanto participar de grupos. Saia de todos os grupos primeiro.");
        }

        userRepository.deleteById(userId);
    }
}
