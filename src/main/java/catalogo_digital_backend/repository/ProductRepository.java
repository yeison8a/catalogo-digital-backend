
package catalogo_digital_backend.repository;

import catalogo_digital_backend.entity.Product;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository
        extends JpaRepository<Product, UUID> {

    // Consultar todos los productos junto con su categoría
    @Override
    @EntityGraph(attributePaths = "category")
    List<Product> findAll();

    // Consultar un producto junto con su categoría
    @Override
    @EntityGraph(attributePaths = "category")
    Optional<Product> findById(UUID id);

    // Productos publicados y su categoría
    @EntityGraph(attributePaths = "category")
    List<Product> findByActiveTrue();

    // Productos publicados y disponibles
    List<Product> findByActiveTrueAndAvailableTrue();

    // Productos de una categoría
    List<Product> findByCategory_Id(UUID categoryId);

    // Búsqueda por nombre
    List<Product> findByNameContainingIgnoreCase(String name);

    // Contar productos publicados de una categoría
    long countByCategory_IdAndActiveTrue(UUID categoryId);
}
