
package catalogo_digital_backend.security;

import catalogo_digital_backend.entity.Role;
import catalogo_digital_backend.entity.User;
import catalogo_digital_backend.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminJwtAuthenticationConverterTest {

    private UserRepository userRepository;
    private AdminJwtAuthenticationConverter converter;
    private UUID userId;
    private User admin;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);

        converter = new AdminJwtAuthenticationConverter(
            userRepository
        );

        userId = UUID.randomUUID();

        admin = new User(
            "admin@example.com",
            "hash-de-prueba",
            Role.ADMIN
        );
    }

    private Jwt createJwt(
            String subject,
            String email,
            String role
    ) {
        return Jwt.withTokenValue("token-de-prueba")
            .header("alg", "HS256")
            .subject(subject)
            .claim("email", email)
            .claim("role", role)
            .build();
    }

    private Jwt validJwt() {
        return createJwt(
            userId.toString(),
            "admin@example.com",
            "ADMIN"
        );
    }

    @Test
    void shouldAuthenticateActiveAdmin() {
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(admin));

        AbstractAuthenticationToken authentication =
            converter.convert(validJwt());

        assertNotNull(authentication);
        assertTrue(authentication.isAuthenticated());

        assertEquals(
            "admin@example.com",
            authentication.getName()
        );

        assertTrue(
            authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                    authority.getAuthority()
                        .equals("ROLE_ADMIN")
                )
        );

        verify(userRepository).findById(userId);
    }

    @Test
    void shouldRejectUnknownUser() {
        when(userRepository.findById(userId))
            .thenReturn(Optional.empty());

        assertThrows(
            BadCredentialsException.class,
            () -> converter.convert(validJwt())
        );
    }

    @Test
    void shouldRejectInactiveAdmin() {
        admin.setActive(false);

        when(userRepository.findById(userId))
            .thenReturn(Optional.of(admin));

        assertThrows(
            BadCredentialsException.class,
            () -> converter.convert(validJwt())
        );
    }

    @Test
    void shouldRejectInvalidSubject() {
        Jwt jwt = createJwt(
            "identificador-invalido",
            "admin@example.com",
            "ADMIN"
        );

        assertThrows(
            BadCredentialsException.class,
            () -> converter.convert(jwt)
        );

        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldRejectIncorrectTokenRole() {
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(admin));

        Jwt jwt = createJwt(
            userId.toString(),
            "admin@example.com",
            "USER"
        );

        assertThrows(
            BadCredentialsException.class,
            () -> converter.convert(jwt)
        );
    }

    @Test
    void shouldRejectDifferentEmail() {
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(admin));

        Jwt jwt = createJwt(
            userId.toString(),
            "otro@example.com",
            "ADMIN"
        );

        assertThrows(
            BadCredentialsException.class,
            () -> converter.convert(jwt)
        );
    }

    @Test
    void shouldRejectMissingEmail() {
        when(userRepository.findById(userId))
            .thenReturn(Optional.of(admin));

        Jwt jwt = createJwt(
            userId.toString(),
            null,
            "ADMIN"
        );

        assertThrows(
            BadCredentialsException.class,
            () -> converter.convert(jwt)
        );
    }
}
