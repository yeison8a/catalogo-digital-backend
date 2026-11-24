
package catalogo_digital_backend.security;

import catalogo_digital_backend.entity.Role;
import catalogo_digital_backend.entity.User;
import catalogo_digital_backend.repository.UserRepository;

import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

/**
 * Autenticación de administradores contra PostgreSQL.
 */
@Service
public class AdminAuthenticationService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;

    public AdminAuthenticationService(
            UserRepository userRepository,
            PasswordService passwordService
    ) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
    }

    /**
     * Devuelve un usuario únicamente si:
     * - El correo existe.
     * - La cuenta está activa.
     * - Tiene el rol ADMIN.
     * - La contraseña coincide con BCrypt.
     */
    public Optional<User> authenticate(
            String email,
            String password
    ) {
        if (email == null || password == null) {
            return Optional.empty();
        }

        String normalizedEmail = email
            .trim()
            .toLowerCase(Locale.ROOT);

        if (
            normalizedEmail.isBlank() ||
            normalizedEmail.length() > 254 ||
            password.isEmpty()
        ) {
            return Optional.empty();
        }

        Optional<User> optionalUser =
            userRepository.findByEmailIgnoreCase(
                normalizedEmail
            );

        if (optionalUser.isEmpty()) {
            return Optional.empty();
        }

        User user = optionalUser.get();

        if (!user.isActive()) {
            return Optional.empty();
        }

        if (user.getRole() != Role.ADMIN) {
            return Optional.empty();
        }

        boolean validPassword =
            passwordService.verifyPassword(
                password,
                user.getPasswordHash()
            );

        if (!validPassword) {
            return Optional.empty();
        }

        return Optional.of(user);
    }

    /**
     * Conservamos este método para no romper
     * las pruebas existentes del paso 45.3.2.
     */
    public boolean verifyCredentials(
            String email,
            String password
    ) {
        return authenticate(email, password)
            .isPresent();
    }
}
