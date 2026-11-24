
package catalogo_digital_backend.security;

import catalogo_digital_backend.entity.Role;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

/**
 * Genera y valida tokens JWT firmados.
 *
 * Todavía no se conecta al endpoint de login.
 */
@Service
@ConditionalOnProperty(name = "app.jwt.secret-base64")
public class JwtService {

    
private static final String ISSUER =
    "https://catalogo-digital.local";


    private static final Duration TOKEN_DURATION =
            Duration.ofMinutes(15);

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;

    public JwtService(
            @Value("${app.jwt.secret-base64}")
            String secretBase64
    ) {
        byte[] keyBytes;

        try {
            keyBytes = Base64.getDecoder()
                    .decode(secretBase64);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                "La clave JWT debe estar codificada en Base64.",
                exception
            );
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                "La clave JWT debe tener al menos 32 bytes."
            );
        }

        SecretKey secretKey = new SecretKeySpec(
            keyBytes,
            "HmacSHA256"
        );

        this.encoder = NimbusJwtEncoder
            .withSecretKey(secretKey)
            .algorithm(MacAlgorithm.HS256)
            .build();

        NimbusJwtDecoder jwtDecoder =
            NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        jwtDecoder.setJwtValidator(
            JwtValidators.createDefaultWithIssuer(ISSUER)
        );

        this.decoder = jwtDecoder;
    }

    /**
     * Genera un JWT para un usuario ya autenticado.
     *
     * Nunca debe llamarse antes de verificar
     * la contraseña y el estado de la cuenta.
     */
    public String generateToken(
            UUID userId,
            String email,
            Role role
    ) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(ISSUER)
            .subject(userId.toString())
            .issuedAt(now)
            .expiresAt(now.plus(TOKEN_DURATION))
            .claim("email", email)
            .claim("role", role.name())
            .build();

        JwsHeader header = JwsHeader
            .with(MacAlgorithm.HS256)
            .build();

        return encoder.encode(
            JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }

    /**
     * Comprueba la firma, el vencimiento
     * y el emisor del JWT.
     *
     * Un token inválido provoca una excepción
     * de validación de Spring Security.
     */
    public Jwt validateToken(String token) {
        return decoder.decode(token);
    }
}
