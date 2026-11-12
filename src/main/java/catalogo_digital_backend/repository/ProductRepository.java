
package catalogo_digital_backend.repository;

import catalogo_digital_backend.entity.Product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRepository
        extends JpaRepository<Product, UUID> {

    // Productos publicados
    List<Product> findByActiveTrue();

    // Productos publicados y disponibles
    List<Product> findByActiveTrueAndAvailableTrue();

    // Productos asociados a una categoría
    List<Product> findByCategory_Id(UUID categoryId);

    // Buscar productos por nombre sin distinguir mayúsculas
    List<Product> findByNameContainingIgnoreCase(String name);

    // Contar productos publicados de una categoría
    long countByCategory_IdAndActiveTrue(UUID categoryId);
}
