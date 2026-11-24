
package catalogo_digital_backend.security;

import catalogo_digital_backend.entity.User;
import catalogo_digital_backend.repository.UserRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Permite a Spring Security obtener usuarios
 * administradores desde PostgreSQL.
 */
@Service
public class AdminUserDetailsService
        implements UserDetailsService {

    private final UserRepository userRepository;

    public AdminUserDetailsService(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(
            String email
    ) throws UsernameNotFoundException {

        if (email == null || email.isBlank()) {
            throw new UsernameNotFoundException(
                "Credenciales inválidas"
            );
        }

        String normalizedEmail = email
            .trim()
            .toLowerCase(Locale.ROOT);

        User user = userRepository
            .findByEmailIgnoreCase(normalizedEmail)
            .orElseThrow(() ->
                new UsernameNotFoundException(
                    "Credenciales inválidas"
                )
            );

        return org.springframework.security.core.userdetails.User
            .withUsername(user.getEmail())
            .password(user.getPasswordHash())
            .roles(user.getRole().name())
            .disabled(!user.isActive())
            .build();
    }
}
