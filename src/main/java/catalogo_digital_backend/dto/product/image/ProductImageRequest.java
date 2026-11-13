
package catalogo_digital_backend.dto.product.image;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductImageRequest(

        @NotBlank(
                message = "La URL de la imagen es obligatoria"
        )
        @Size(
                max = 2048,
                message = "La URL no puede superar 2048 caracteres"
        )
        String imageUrl

) {
}
