
package catalogo_digital_backend.dto.auth;

/**
 * Resultado de la autenticación.
 *
 * No incluye contraseñas ni hashes.
 */
public record LoginResponse(
    boolean authenticated,
    String message,
    String accessToken,
    String tokenType,
    long expiresIn
) {
}
