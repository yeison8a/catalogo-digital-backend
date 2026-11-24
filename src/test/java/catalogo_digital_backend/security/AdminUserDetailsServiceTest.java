
package catalogo_digital_backend.security;

import catalogo_digital_backend.entity.Role;
import catalogo_digital_backend.entity.User;
import catalogo_digital_backend.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminUserDetailsServiceTest {

    private UserRepository userRepository;
    private AdminUserDetailsService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);

        service = new AdminUserDetailsService(
            userRepository
        );
    }

    @Test
    void shouldLoadAdministratorFromDatabase() {

        User user = new User(
            "admin@example.com",
            "hash-de-prueba",
            Role.ADMIN
        );

        when(
            userRepository.findByEmailIgnoreCase(
                "admin@example.com"
            )
        ).thenReturn(Optional.of(user));

        UserDetails details =
            service.loadUserByUsername(
                "ADMIN@example.com"
            );

        assertEquals(
            "admin@example.com",
            details.getUsername()
        );

        assertEquals(
            "hash-de-prueba",
            details.getPassword()
        );

        assertTrue(details.isEnabled());

        assertTrue(
            details.getAuthorities()
                .stream()
                .anyMatch(authority ->
                    authority.getAuthority()
                        .equals("ROLE_ADMIN")
                )
        );
    }

    @Test
    void shouldRejectUnknownUser() {

        when(
            userRepository.findByEmailIgnoreCase(
                "unknown@example.com"
            )
        ).thenReturn(Optional.empty());

        assertThrows(
            UsernameNotFoundException.class,
            () -> service.loadUserByUsername(
                "unknown@example.com"
            )
        );
    }

    @Test
    void shouldMarkInactiveUserAsDisabled() {

        User user = new User(
            "inactive@example.com",
            "hash-de-prueba",
            Role.ADMIN
        );

        user.setActive(false);

        when(
            userRepository.findByEmailIgnoreCase(
                "inactive@example.com"
            )
        ).thenReturn(Optional.of(user));

        UserDetails details =
            service.loadUserByUsername(
                "inactive@example.com"
            );

        assertFalse(details.isEnabled());
    }
}
