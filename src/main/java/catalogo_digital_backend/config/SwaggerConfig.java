
package catalogo_digital_backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

import org.springdoc.core.models.GroupedOpenApi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
public class SwaggerConfig {

    @Bean
    public OpenAPI catalogoDigitalOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Catálogo Digital - API REST")
                        .version("1.0.0")
                        .description("""
                                API REST desarrollada con Spring Boot.

                                Funcionalidades:
                                - Administración de categorías.
                                - Administración de productos.
                                - Publicación y disponibilidad.
                                - Fotografías almacenadas en Cloudinary.
                                - Eliminaciones pendientes con reintentos.

                                Backend: Spring Boot 4.
                                Base de datos: PostgreSQL.
                                Almacenamiento: Cloudinary.

                                Los endpoints administrativos solo están
                                habilitados en el perfil local durante
                                esta etapa del proyecto.
                                """)
                        .contact(new Contact()
                                .name("Proyecto Catálogo Digital"))
                        .license(new License()
                                .name("Uso interno del proyecto")));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("01 - API Pública")
                .pathsToMatch("/api/v1/**")
                .pathsToExclude("/api/v1/admin/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("02 - Administración")
                .pathsToMatch("/api/v1/admin/**")
                .build();
    }
}
