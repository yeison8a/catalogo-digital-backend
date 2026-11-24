
package catalogo_digital_backend.security;

import catalogo_digital_backend.entity.Role;
import catalogo_digital_backend.entity.User;
import catalogo_digital_backend.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Convierte un JWT validado en una autenticación
 * de Spring Security.
 *
 * Consulta PostgreSQL en cada autenticación Bearer
 * para verificar que la cuenta siga autorizada.
 */
@Component
@ConditionalOnProperty(name = "app.jwt.secret-base64")
public class AdminJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    public AdminJwtAuthenticationConverter(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {

        // El subject del JWT debe contener el UUID.
        UUID userId;

        try {
            userId = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new BadCredentialsException(
                "Token no autorizado"
            );
        }

        // Consultar el usuario actual en PostgreSQL.
        Optional<User> optionalUser =
            userRepository.findById(userId);

        if (optionalUser.isEmpty()) {
            throw new BadCredentialsException(
                "Token no autorizado"
            );
        }

        User user = optionalUser.get();

        // Un administrador desactivado pierde acceso.
        if (!user.isActive()) {
            throw new BadCredentialsException(
                "Token no autorizado"
            );
        }

        // El rol debe estar vigente en PostgreSQL.
        if (user.getRole() != Role.ADMIN) {
            throw new BadCredentialsException(
                "Token no autorizado"
            );
        }

        // El token también debe declarar el rol esperado.
        String tokenRole = jwt.getClaimAsString("role");

        if (!Role.ADMIN.name().equals(tokenRole)) {
            throw new BadCredentialsException(
                "Token no autorizado"
            );
        }

        // Comprobar coherencia entre JWT y PostgreSQL.
        String tokenEmail = jwt.getClaimAsString("email");

        if (
            tokenEmail == null ||
            !user.getEmail().equalsIgnoreCase(tokenEmail)
        ) {
            throw new BadCredentialsException(
                "Token no autorizado"
            );
        }

        // Solo después de validar todo asignamos
        // la autoridad administrativa.
        return new JwtAuthenticationToken(
            jwt,
            List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN")
            ),
            user.getEmail()
        );
    }
}
