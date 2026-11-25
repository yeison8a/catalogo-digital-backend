
package catalogo_digital_backend.security;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import static org.junit.jupiter.api.Assertions.*;

class JwtSecurityValidationTest {

    private JwtService jwtService;
    private JwtEncoder testEncoder;

    @BeforeEach
    void setUp() {

        byte[] secretBytes = new byte[32];

        new SecureRandom().nextBytes(secretBytes);

        String secretBase64 = Base64.getEncoder()
            .encodeToString(secretBytes);

        jwtService = new JwtService(secretBase64);

        SecretKey secretKey = new SecretKeySpec(
            secretBytes,
            "HmacSHA256"
        );

        testEncoder = NimbusJwtEncoder
            .withSecretKey(secretKey)
            .algorithm(MacAlgorithm.HS256)
            .build();
    }

    /**
     * Crea un JWT firmado con la misma clave
     * que JwtService, pero permite controlar
     * las fechas y el emisor de las pruebas.
     */
    private String createToken(
            String issuer,
            Instant issuedAt,
            Instant expiresAt
    ) {

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .subject("123e4567-e89b-12d3-a456-426614174000")
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .claim("email", "admin@example.com")
            .claim("role", "ADMIN")
            .build();

        JwsHeader header = JwsHeader
            .with(MacAlgorithm.HS256)
            .build();

        return testEncoder.encode(
            JwtEncoderParameters.from(
                header,
                claims
            )
        ).getTokenValue();
    }

    @Test
    void shouldRejectExpiredToken() {

        Instant now = Instant.now();

        String expiredToken = createToken(
            "https://catalogo-digital.local",
            now.minusSeconds(1800),
            now.minusSeconds(600)
        );

        assertThrows(
            JwtException.class,
            () -> jwtService.validateToken(expiredToken)
        );
    }

    @Test
    void shouldRejectDifferentIssuer() {

        Instant now = Instant.now();

        String token = createToken(
            "https://otro-emisor.example",
            now,
            now.plusSeconds(900)
        );

        assertThrows(
            JwtException.class,
            () -> jwtService.validateToken(token)
        );
    }

    @Test
    void shouldRejectMalformedToken() {

        assertThrows(
            JwtException.class,
            () -> jwtService.validateToken(
                "esto-no-es-un-jwt"
            )
        );
    }
}
