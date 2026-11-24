
package catalogo_digital_backend.security;

import catalogo_digital_backend.entity.Role;
import catalogo_digital_backend.entity.User;
import catalogo_digital_backend.repository.UserRepository;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Permite crear el primer administrador
 * exclusivamente mediante un inicio controlado
 * de la aplicación en el perfil local.
 *
 * No expone endpoints HTTP.
 */
@Component
@Profile("local")
@ConditionalOnProperty(
    name = "app.admin-bootstrap.enabled",
    havingValue = "true"
)
public class LocalAdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordService passwordService;

    public LocalAdminBootstrap(
            UserRepository userRepository,
            PasswordService passwordService
    ) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
    }

    @Override
    public void run(ApplicationArguments args) {

        // Impedir crear otro administrador.
        if (userRepository.count() > 0) {
            throw new IllegalStateException(
                "Ya existen usuarios registrados. " +
                "La creación inicial está deshabilitada."
            );
        }

        // Credenciales proporcionadas únicamente
        // mediante variables de entorno.
        String email = System.getenv(
            "ADMIN_BOOTSTRAP_EMAIL"
        );

        String password = System.getenv(
            "ADMIN_BOOTSTRAP_PASSWORD"
        );

        validateCredentials(email, password);

        String normalizedEmail = email
            .trim()
            .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(
                normalizedEmail)) {
            throw new IllegalStateException(
                "El correo ya está registrado."
            );
        }

        // Nunca guardar la contraseña original.
        String passwordHash =
            passwordService.hashPassword(password);

        User administrator = new User(
            normalizedEmail,
            passwordHash,
            Role.ADMIN
        );

        userRepository.saveAndFlush(administrator);

        System.out.println(
            "Administrador inicial creado correctamente."
        );
    }

    /**
     * Validaciones previas a la creación.
     */
    private void validateCredentials(
            String email,
            String password
    ) {
        if (
            email == null ||
            email.isBlank() ||
            email.length() > 254 ||
            !email.trim().matches(
                "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
            )
        ) {
            throw new IllegalArgumentException(
                "El correo del administrador no es válido."
            );
        }

        if (password == null) {
            throw new IllegalArgumentException(
                "Debe proporcionar una contraseña."
            );
        }

        int passwordBytes = password
            .getBytes(StandardCharsets.UTF_8)
            .length;

        if (
            passwordBytes < 12 ||
            passwordBytes > 72
        ) {
            throw new IllegalArgumentException(
                "La contraseña debe tener entre " +
                "12 y 72 bytes en UTF-8."
            );
        }
    }
}
