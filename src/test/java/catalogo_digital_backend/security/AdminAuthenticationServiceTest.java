
package catalogo_digital_backend.security;

import catalogo_digital_backend.entity.Role;
import catalogo_digital_backend.entity.User;
import catalogo_digital_backend.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminAuthenticationServiceTest {

    private UserRepository userRepository;
    private PasswordService passwordService;
    private AdminAuthenticationService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordService = mock(PasswordService.class);

        service = new AdminAuthenticationService(
            userRepository,
            passwordService
        );
    }

    @Test
    void shouldAcceptValidCredentials() {
        User user = new User(
            "admin@example.com",
            "hash-de-prueba",
            Role.ADMIN
        );

        when(userRepository.findByEmailIgnoreCase(
            "admin@example.com"
        )).thenReturn(Optional.of(user));

        when(passwordService.verifyPassword(
            "ClaveCorrectaDePrueba",
            "hash-de-prueba"
        )).thenReturn(true);

        assertTrue(service.verifyCredentials(
            "ADMIN@example.com",
            "ClaveCorrectaDePrueba"
        ));
    }

    @Test
    void shouldRejectWrongPassword() {
        User user = new User(
            "admin@example.com",
            "hash-de-prueba",
            Role.ADMIN
        );

        when(userRepository.findByEmailIgnoreCase(
            "admin@example.com"
        )).thenReturn(Optional.of(user));

        assertFalse(service.verifyCredentials(
            "admin@example.com",
            "ClaveIncorrecta"
        ));
    }

    @Test
    void shouldRejectUnknownUser() {
        when(userRepository.findByEmailIgnoreCase(
            "unknown@example.com"
        )).thenReturn(Optional.empty());

        assertFalse(service.verifyCredentials(
            "unknown@example.com",
            "ClaveDePrueba"
        ));
    }

    @Test
    void shouldRejectInactiveUser() {
        User user = new User(
            "inactive@example.com",
            "hash-de-prueba",
            Role.ADMIN
        );

        user.setActive(false);

        when(userRepository.findByEmailIgnoreCase(
            "inactive@example.com"
        )).thenReturn(Optional.of(user));

        assertFalse(service.verifyCredentials(
            "inactive@example.com",
            "ClaveDePrueba"
        ));

        verifyNoInteractions(passwordService);
    }

    @Test
    void shouldRejectMissingCredentials() {
        assertFalse(service.verifyCredentials(
            null,
            "ClaveDePrueba"
        ));

        assertFalse(service.verifyCredentials(
            "admin@example.com",
            null
        ));

        assertFalse(service.verifyCredentials(
            " ",
            "ClaveDePrueba"
        ));
    }
}
