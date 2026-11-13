
package catalogo_digital_backend.repository;

import catalogo_digital_backend.entity.Product;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository
        extends JpaRepository<Product, UUID> {

    // Consultar todos los productos con su categoría
    @Override
    @EntityGraph(attributePaths = "category")
    List<Product> findAll();

    // Consultar un producto con su categoría
    @Override
    @EntityGraph(attributePaths = "category")
    Optional<Product> findById(UUID id);

    // Productos publicados
    @EntityGraph(attributePaths = "category")
    List<Product> findByActiveTrue();

    // Productos publicados y disponibles
    List<Product> findByActiveTrueAndAvailableTrue();

    // Productos de una categoría
    List<Product> findByCategory_Id(UUID categoryId);

    // Buscar por nombre
    List<Product> findByNameContainingIgnoreCase(String name);

    // Contar productos activos de una categoría
    long countByCategory_IdAndActiveTrue(UUID categoryId);

    // NUEVO: bloquear un producto para modificar
    // su galería de fotografías
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           SELECT p
           FROM Product p
           WHERE p.id = :productId
           """)
    Optional<Product> findByIdForUpdate(
            @Param("productId") UUID productId
    );
}
