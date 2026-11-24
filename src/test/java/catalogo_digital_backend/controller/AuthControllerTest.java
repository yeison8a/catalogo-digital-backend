
package catalogo_digital_backend.controller;

import catalogo_digital_backend.dto.auth.LoginRequest;
import catalogo_digital_backend.dto.auth.LoginResponse;
import catalogo_digital_backend.entity.Role;
import catalogo_digital_backend.entity.User;
import catalogo_digital_backend.security.AdminAuthenticationService;
import catalogo_digital_backend.security.JwtService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    private AdminAuthenticationService authenticationService;
    private JwtService jwtService;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        authenticationService =
            mock(AdminAuthenticationService.class);

        jwtService = mock(JwtService.class);

        controller = new AuthController(
            authenticationService,
            jwtService
        );
    }

    @Test
    void shouldReturnTokenWhenCredentialsAreValid() {
        User user = new User(
            "admin@example.com",
            "hash-de-prueba",
            Role.ADMIN
        );

        when(authenticationService.authenticate(
            "admin@example.com",
            "ClaveCorrectaDePrueba"
        )).thenReturn(Optional.of(user));

        when(jwtService.generateToken(
            nullable(UUID.class),
            eq("admin@example.com"),
            eq(Role.ADMIN)
        )).thenReturn("jwt-de-prueba");

        when(jwtService.getExpiresInSeconds())
            .thenReturn(900L);

        LoginRequest request = new LoginRequest(
            "admin@example.com",
            "ClaveCorrectaDePrueba"
        );

        ResponseEntity<LoginResponse> response =
            controller.login(request);

        assertEquals(
            HttpStatus.OK,
            response.getStatusCode()
        );

        LoginResponse body = response.getBody();

        assertNotNull(body);
        assertTrue(body.authenticated());

        assertEquals(
            "jwt-de-prueba",
            body.accessToken()
        );

        assertEquals("Bearer", body.tokenType());
        assertEquals(900L, body.expiresIn());

        verify(jwtService).generateToken(
            nullable(UUID.class),
            eq("admin@example.com"),
            eq(Role.ADMIN)
        );
    }

    @Test
    void shouldRejectInvalidCredentialsWithoutToken() {
        when(authenticationService.authenticate(
            "admin@example.com",
            "ClaveIncorrecta"
        )).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest(
            "admin@example.com",
            "ClaveIncorrecta"
        );

        ResponseEntity<LoginResponse> response =
            controller.login(request);

        assertEquals(
            HttpStatus.UNAUTHORIZED,
            response.getStatusCode()
        );

        LoginResponse body = response.getBody();

        assertNotNull(body);
        assertFalse(body.authenticated());
        assertNull(body.accessToken());

        verifyNoInteractions(jwtService);
    }
}
