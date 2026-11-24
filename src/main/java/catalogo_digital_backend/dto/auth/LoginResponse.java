
package catalogo_digital_backend.dto.auth;

/**
 * Respuesta temporal de verificación
 * de credenciales.
 *
 * El JWT se incorporará posteriormente.
 */
public record LoginResponse(
    boolean authenticated,
    String message
) {
}
