
package catalogo_digital_backend.dto.auth;

/**
 * Datos que recibe el endpoint de login.
 *
 * Nunca debemos registrar la contraseña
 * en logs ni devolverla en respuestas.
 */
public record LoginRequest(
    String email,
    String password
) {
}
