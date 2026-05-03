package portifolio.deuquanto.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import portifolio.deuquanto.configuration.DataMasker;
import portifolio.deuquanto.dto.request.LoginRequest;
import portifolio.deuquanto.dto.response.LoginResponse;
import portifolio.deuquanto.entity.PasswordResetToken;
import portifolio.deuquanto.entity.Users;
import portifolio.deuquanto.exception.BusinessException;
import portifolio.deuquanto.repository.PasswordResetTokenRepository;
import portifolio.deuquanto.repository.UserRepository;
import portifolio.deuquanto.security.TokenConfig;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final TokenConfig tokenConfig;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository, TokenConfig tokenConfig, PasswordResetTokenRepository passwordResetTokenRepository, EmailService emailService, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.tokenConfig = tokenConfig;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest loginRequest){
        UsernamePasswordAuthenticationToken userPassAuth = new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password());
        Authentication authentication = authenticationManager.authenticate(userPassAuth);

        Users user = (Users) authentication.getPrincipal();
        String token = tokenConfig.generateToken(Objects.requireNonNull(user));
        log.info("Login realizado pelo usuario: {}", user.getId());
        return new LoginResponse(token);
    }

    @Transactional
    public void requestPasswordReset(String email) {
        Optional<Users> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            log.warn("Tentativa de reset para e-mail não cadastrado: {}", DataMasker.maskEmail(email));
            return;
        }

        Users user = userOpt.get();

        Optional<PasswordResetToken> lastTokenOpt = passwordResetTokenRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId());

        if (lastTokenOpt.isPresent()) {
            Instant dataCriacaoDoUltimoToken = lastTokenOpt.get().getCreatedAt();
            if (dataCriacaoDoUltimoToken.plus(5, ChronoUnit.MINUTES).isAfter(Instant.now())) {
                log.warn("Cooldown ativo! Ignorando disparo de e-mail para o usuário: {}", user.getId());
                return;
            }
        }

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));

        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), token);

        log.info("E-mail de recuperação de senha enviado com sucesso para o usuário: {}", user.getId());
    }


    public PasswordResetToken validateResetToken(String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new EntityNotFoundException("Link de recuperação inválido ou inexistente."));

        if (resetToken.isUsed()) {
            throw new BusinessException("Este link já foi utilizado. Solicite um novo.");
        }

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Este link de recuperação expirou. Solicite um novo.");
        }

        return resetToken;
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = validateResetToken(token);

        Users user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Senha redefinida com sucesso para o usuário: {}", user.getId());
    }
}
