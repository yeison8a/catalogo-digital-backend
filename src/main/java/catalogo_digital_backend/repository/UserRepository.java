
package catalogo_digital_backend.repository;

import catalogo_digital_backend.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de usuarios administradores.
 *
 * Spring Data JPA proporciona automáticamente
 * las operaciones básicas de persistencia.
 */
public interface UserRepository
        extends JpaRepository<User, UUID> {

    /**
     * Busca un usuario mediante su correo.
     *
     * Se utilizará durante el login.
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Comprueba si ya existe un correo.
     */
    boolean existsByEmailIgnoreCase(String email);
}
