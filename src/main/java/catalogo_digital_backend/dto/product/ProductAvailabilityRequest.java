
package catalogo_digital_backend.dto.product;

import jakarta.validation.constraints.NotNull;

public record ProductAvailabilityRequest(

        @NotNull(
                message = "La disponibilidad es obligatoria"
        )
        Boolean available

) {
}
