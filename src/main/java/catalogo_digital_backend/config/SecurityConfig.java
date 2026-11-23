
package catalogo_digital_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
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

                // Bloquear temporalmente toda administración.
                // Después exigiremos el rol ADMIN con JWT.
                .requestMatchers("/api/v1/admin/**")
                .denyAll()

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

        return http.build();
    }
}
