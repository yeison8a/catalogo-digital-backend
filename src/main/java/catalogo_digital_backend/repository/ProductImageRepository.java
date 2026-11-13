
package catalogo_digital_backend.repository;

import catalogo_digital_backend.entity.ProductImage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductImageRepository
        extends JpaRepository<ProductImage, UUID> {

    // Fotografías de un producto ordenadas
    List<ProductImage> findByProduct_IdOrderBySortOrderAsc(
            UUID productId
    );

    // Imagen principal de un único producto
    Optional<ProductImage> findByProduct_IdAndMainImageTrue(
            UUID productId
    );

    // Imágenes principales de múltiples productos
    List<ProductImage> findByProduct_IdInAndMainImageTrue(
            Collection<UUID> productIds
    );

    // Contar fotografías de un producto
    long countByProduct_Id(UUID productId);
}
