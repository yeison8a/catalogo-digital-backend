
package catalogo_digital_backend.dto.product;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        boolean available,
        boolean active,
        UUID categoryId,
        String categoryName,
        String mainImageUrl
) {
}
