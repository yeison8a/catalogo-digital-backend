
package catalogo_digital_backend.dto.product.image;

import java.util.UUID;

public record ProductImageResponse(
        UUID id,
        String imageUrl,
        int sortOrder,
        boolean mainImage,
        UUID productId
) {
}
