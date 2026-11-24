
package catalogo_digital_backend.security;

import catalogo_digital_backend.entity.Role;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    private String generateTestSecret() {
        byte[] bytes = new byte[32];

        new SecureRandom().nextBytes(bytes);

        return Base64.getEncoder()
            .encodeToString(bytes);
    }

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
            generateTestSecret()
        );
    }

    @Test
    void shouldGenerateAndValidateToken() {
        UUID userId = UUID.randomUUID();

        String token = jwtService.generateToken(
            userId,
            "admin@example.com",
            Role.ADMIN
        );

        Jwt jwt = jwtService.validateToken(token);

        assertEquals(
            userId.toString(),
            jwt.getSubject()
        );

        assertEquals(
            "admin@example.com",
            jwt.getClaimAsString("email")
        );

        assertEquals(
            "ADMIN",
            jwt.getClaimAsString("role")
        );

        
assertEquals(
    "https://catalogo-digital.local",
    jwt.getIssuer().toString()
);


        assertNotNull(jwt.getExpiresAt());

        assertTrue(
            jwt.getExpiresAt().isAfter(Instant.now())
        );
    }

    @Test
    void shouldRejectTamperedToken() {
        String token = jwtService.generateToken(
            UUID.randomUUID(),
            "admin@example.com",
            Role.ADMIN
        );

        String[] parts = token.split("\\.");

        // Modificar parte de la firma.
        String signature = parts[2];

        char replacement =
            signature.charAt(0) == 'A' ? 'B' : 'A';

        parts[2] = replacement
            + signature.substring(1);

        String tamperedToken = String.join(".", parts);

        assertThrows(
            JwtException.class,
            () -> jwtService.validateToken(tamperedToken)
        );
    }

    @Test
    void shouldRejectTokenSignedWithAnotherSecret() {
        JwtService anotherService = new JwtService(
            generateTestSecret()
        );

        String token = jwtService.generateToken(
            UUID.randomUUID(),
            "admin@example.com",
            Role.ADMIN
        );

        assertThrows(
            JwtException.class,
            () -> anotherService.validateToken(token)
        );
    }

    @Test
    void shouldRejectWeakSecret() {
        String shortSecret = Base64.getEncoder()
            .encodeToString(new byte[16]);

        assertThrows(
            IllegalStateException.class,
            () -> new JwtService(shortSecret)
        );
    }
}
