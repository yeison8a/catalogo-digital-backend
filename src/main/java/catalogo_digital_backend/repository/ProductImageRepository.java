
package catalogo_digital_backend.repository;

import catalogo_digital_backend.entity.ProductImage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductImageRepository
        extends JpaRepository<ProductImage, UUID> {

    // Fotografías de un producto ordenadas por posición
    List<ProductImage> findByProduct_IdOrderBySortOrderAsc(
            UUID productId
    );

    // Buscar la imagen principal de un producto
    Optional<ProductImage> findByProduct_IdAndMainImageTrue(
            UUID productId
    );

    // Contar fotografías de un producto
    long countByProduct_Id(UUID productId);
}
