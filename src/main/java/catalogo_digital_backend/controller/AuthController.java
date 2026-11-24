
package catalogo_digital_backend.controller;

import catalogo_digital_backend.dto.auth.LoginRequest;
import catalogo_digital_backend.dto.auth.LoginResponse;
import catalogo_digital_backend.security.AdminAuthenticationService;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Endpoint temporal de comprobación
 * de credenciales para desarrollo local.
 *
 * Todavía no emite JWT ni crea sesiones.
 */
@RestController
@Profile("local")
@RequestMapping("/api/v1/auth")
@Tag(
    name = "Autenticación",
    description = "Verificación de credenciales administrativas"
)
public class AuthController {

    private final AdminAuthenticationService authenticationService;

    public AuthController(
            AdminAuthenticationService authenticationService
    ) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request
    ) {
        boolean valid =
            authenticationService.verifyCredentials(
                request.email(),
                request.password()
            );

        if (!valid) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(
                    new LoginResponse(
                        false,
                        "Credenciales inválidas."
                    )
                );
        }

        return ResponseEntity.ok(
            new LoginResponse(
                true,
                "Credenciales verificadas. " +
                "Todavía no se ha emitido un JWT."
            )
        );
    }
}
