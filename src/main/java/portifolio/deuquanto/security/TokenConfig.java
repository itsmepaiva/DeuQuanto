package portifolio.deuquanto.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import portifolio.deuquanto.dto.JWTUserData;
import portifolio.deuquanto.entity.Users;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class TokenConfig {

    @Value("${api.secret.value.token}")
    private String secret;

    public String generateToken(Users user){
        Algorithm algorithm = Algorithm.HMAC256(secret);

        return JWT.create()
                .withClaim("userId", user.getId().toString())
                .withSubject(user.getEmail())
                .withExpiresAt(Instant.now().plusSeconds(86400))
                .withIssuedAt(Instant.now())
                .sign(algorithm);
    }

    public Optional<JWTUserData> validateToken(String token){
        try{
            Algorithm algorithm = Algorithm.HMAC256(secret);

            DecodedJWT decodedJWT = JWT.require(algorithm)
                    .build().verify(token);

            return Optional.of(JWTUserData.builder()
                    .userId(UUID.fromString(decodedJWT.getClaim("userId").asString()))
                    .email(decodedJWT.getSubject())
                    .build());
        }
        catch (JWTVerificationException ex){
            return Optional.empty();
        }
    }

}
