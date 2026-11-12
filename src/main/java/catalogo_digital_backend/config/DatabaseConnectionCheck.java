
package catalogo_digital_backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseConnectionCheck {

    private static final Logger logger =
            LoggerFactory.getLogger(DatabaseConnectionCheck.class);

    @Bean
    public ApplicationRunner verifyDatabaseConnection(
            JdbcTemplate jdbcTemplate
    ) {
        return args -> {

            String databaseName = jdbcTemplate.queryForObject(
                    "SELECT current_database()",
                    String.class
            );

            String databaseUser = jdbcTemplate.queryForObject(
                    "SELECT current_user",
                    String.class
            );

            logger.info(
                    "Conexión PostgreSQL exitosa. Base de datos: {}, Usuario: {}",
                    databaseName,
                    databaseUser
            );
        };
    }
}
