
package catalogo_digital_backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class PasswordServiceTest {

    private final PasswordService passwordService =
            new PasswordService(
                    new BCryptPasswordEncoder(12)
            );

    @Test
    void shouldHashAndVerifyPassword() {
        String password = "ClaveDePrueba2026!";

        String hash =
                passwordService.hashPassword(password);

        assertNotEquals(password, hash);

        assertTrue(
                passwordService.verifyPassword(
                        password,
                        hash
                )
        );
    }

    @Test
    void shouldRejectIncorrectPassword() {
        String hash =
                passwordService.hashPassword(
                        "ClaveCorrecta2026!"
                );

        assertFalse(
                passwordService.verifyPassword(
                        "ClaveIncorrecta2026!",
                        hash
                )
        );
    }

    @Test
    void shouldGenerateDifferentHashes() {
        String password = "ClaveDePrueba2026!";

        String firstHash =
                passwordService.hashPassword(password);

        String secondHash =
                passwordService.hashPassword(password);

        assertNotEquals(firstHash, secondHash);

        assertTrue(
                passwordService.verifyPassword(
                        password,
                        firstHash
                )
        );

        assertTrue(
                passwordService.verifyPassword(
                        password,
                        secondHash
                )
        );
    }
}
