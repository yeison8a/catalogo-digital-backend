

package catalogo_digital_backend.controller;

import catalogo_digital_backend.dto.auth.LoginRequest;
import catalogo_digital_backend.dto.auth.LoginResponse;
import catalogo_digital_backend.entity.User;
import catalogo_digital_backend.security.AdminAuthenticationService;
import catalogo_digital_backend.security.JwtService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Optional;

/**
 * Autenticación de administradores.
 *
 * En esta etapa el endpoint solo está disponible
 * en el perfil local y con una clave JWT configurada.
 */
@RestController
@Profile("local")
@ConditionalOnProperty(
    name = "app.jwt.secret-base64"
)
@RequestMapping("/api/v1/auth")
@Tag(
    name = "Autenticación",
    description = "Inicio de sesión de administradores"
)
public class AuthController {

    private final AdminAuthenticationService
        authenticationService;

    private final JwtService jwtService;

    public AuthController(
            AdminAuthenticationService authenticationService,
            JwtService jwtService
    ) {
        this.authenticationService =
            authenticationService;

        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request
    ) {
        Optional<User> authenticatedUser =
            authenticationService.authenticate(
                request.email(),
                request.password()
            );

        if (authenticatedUser.isEmpty()) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(
                    new LoginResponse(
                        false,
                        "Credenciales inválidas.",
                        null,
                        null,
                        0
                    )
                );
        }

        User user = authenticatedUser.get();

        String accessToken =
            jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole()
            );

        return ResponseEntity.ok(
            new LoginResponse(
                true,
                "Inicio de sesión correcto.",
                accessToken,
                "Bearer",
                jwtService.getExpiresInSeconds()
            )
        );
    }
}
