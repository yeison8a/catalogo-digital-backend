
package catalogo_digital_backend.dto.product;

import jakarta.validation.constraints.NotNull;

public record ProductPublicationRequest(

        @NotNull(
                message = "El estado de publicación es obligatorio"
        )
        Boolean active

) {
}
