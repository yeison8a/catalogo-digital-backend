
package catalogo_digital_backend.repository;

import catalogo_digital_backend.entity.Category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository
        extends JpaRepository<Category, UUID> {

    // Consultar categorías activas ordenadas por nombre
    List<Category> findByActiveTrueOrderByNameAsc();

    // Buscar una categoría sin distinguir mayúsculas
    boolean existsByNameIgnoreCase(String name);
}
