
package catalogo_digital_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuración del sistema de contraseñas.
 */
@Configuration
public class PasswordConfig {

    /**
     * BCrypt con factor de trabajo 12.
     *
     * El hash incorpora una sal aleatoria.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
