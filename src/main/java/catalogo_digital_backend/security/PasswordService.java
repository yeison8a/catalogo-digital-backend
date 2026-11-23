
package catalogo_digital_backend.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Servicio para generar y verificar
 * hashes de contraseñas.
 */
@Service
public class PasswordService {

    private final PasswordEncoder passwordEncoder;

    public PasswordService(
            PasswordEncoder passwordEncoder
    ) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Genera el hash que almacenaremos
     * en la columna password_hash.
     */
    public String hashPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * Comprueba una contraseña ingresada
     * contra el hash almacenado.
     */
    public boolean verifyPassword(
            String rawPassword,
            String passwordHash
    ) {
        return passwordEncoder.matches(
            rawPassword,
            passwordHash
        );
    }
}
