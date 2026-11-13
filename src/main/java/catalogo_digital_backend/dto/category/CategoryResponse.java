
package catalogo_digital_backend.dto.category;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        boolean active
) {
}
