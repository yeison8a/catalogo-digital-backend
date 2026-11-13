
package catalogo_digital_backend.dto.product.image;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ProductImageOrderRequest(

        @NotNull(message = "La lista de imágenes es obligatoria")
        @NotEmpty(message = "La lista de imágenes no puede estar vacía")
        List<@NotNull UUID> imageIds

) {
}
