
package catalogo_digital_backend.security;

import catalogo_digital_backend.entity.User;
import catalogo_digital_backend.repository.UserRepository;

import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

/**
 * Comprueba las credenciales de administradores
 * almacenados en PostgreSQL.
 *
 * Todavía no genera sesiones ni JWT.
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

    public boolean verifyCredentials(
            String email,
            String password
    ) {
        // Rechazar datos ausentes o inválidos.
        if (email == null || password == null) {
            return false;
        }

        String normalizedEmail = email
            .trim()
            .toLowerCase(Locale.ROOT);

        if (
            normalizedEmail.isBlank() ||
            normalizedEmail.length() > 254 ||
            password.isEmpty()
        ) {
            return false;
        }

        // Consultar el usuario real.
        Optional<User> optionalUser =
            userRepository.findByEmailIgnoreCase(
                normalizedEmail
            );

        if (optionalUser.isEmpty()) {
            return false;
        }

        User user = optionalUser.get();

        // Una cuenta desactivada no puede entrar.
        if (!user.isActive()) {
            return false;
        }

        // Comparar la contraseña ingresada
        // con el hash BCrypt de PostgreSQL.
        return passwordService.verifyPassword(
            password,
            user.getPasswordHash()
        );
    }
}
