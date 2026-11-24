
package catalogo_digital_backend.config;

import catalogo_digital_backend.security.JwtService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Registra el validador JWT utilizado
 * por Spring Security Resource Server.
 *
 * Reutiliza el mismo decoder de JwtService,
 * con la misma clave y las mismas validaciones.
 */
@Configuration
@ConditionalOnProperty(
    name = "app.jwt.secret-base64"
)
public class JwtDecoderConfig {

    @Bean
    public JwtDecoder jwtDecoder(
            JwtService jwtService
    ) {
        return jwtService.getDecoder();
    }
}
