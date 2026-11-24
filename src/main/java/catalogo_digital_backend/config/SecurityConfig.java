
package catalogo_digital_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;


import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.jwt.JwtDecoder;


import catalogo_digital_backend.security.AdminJwtAuthenticationConverter;


@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectProvider<JwtDecoder> jwtDecoderProvider,
        ObjectProvider<AdminJwtAuthenticationConverter>
                adminConverterProvider
    ) throws Exception {

        http
            // API REST sin sesiones HTTP de Spring.
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            // Preparación para autenticación Bearer JWT.
            // No se usarán cookies de sesión para autenticar
            // directamente las peticiones a esta API.
            .csrf(csrf -> csrf.disable())

            // Desactivar los mecanismos de acceso
            // predeterminados mientras construimos JWT.
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())

            .authorizeHttpRequests(auth -> auth

                // Permitir la verificación de credenciales.
// El controlador existe solo con el perfil local.
.requestMatchers(
    HttpMethod.POST,
    "/api/v1/auth/login"
)
.permitAll()

                // Bloquear temporalmente toda administración.
                // Después exigiremos el rol ADMIN con JWT.
                .requestMatchers("/api/v1/admin/**")
                .hasRole("ADMIN")

                // Catálogo público: solo consultas.
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/products",
                    "/api/v1/products/**",
                    "/api/v1/categories"
                )
                .permitAll()

                // Estado del servicio.
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/health"
                )
                .permitAll()

                // Documentación durante el desarrollo.
                // Restringiremos su exposición antes
                // de desplegar en producción.
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                )
                .permitAll()

                // Denegar cualquier otra ruta.
                .anyRequest()
                .denyAll()
            );


// Recuperar los componentes JWT si están configurados.
JwtDecoder jwtDecoder =
    jwtDecoderProvider.getIfAvailable();

AdminJwtAuthenticationConverter adminConverter =
    adminConverterProvider.getIfAvailable();

// Activar autenticación Bearer solo cuando
// estén disponibles ambos componentes.
if (jwtDecoder != null && adminConverter != null) {

    http.oauth2ResourceServer(oauth2 ->
        oauth2.jwt(jwt -> jwt
            .decoder(jwtDecoder)
            .jwtAuthenticationConverter(adminConverter)
        )
    );
}

return http.build();
    }
}
