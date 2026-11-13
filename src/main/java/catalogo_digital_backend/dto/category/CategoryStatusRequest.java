
package catalogo_digital_backend.dto.category;

import jakarta.validation.constraints.NotNull;

public record CategoryStatusRequest(

        @NotNull(
                message = "El estado de publicación es obligatorio"
        )
        Boolean active

) {
}
