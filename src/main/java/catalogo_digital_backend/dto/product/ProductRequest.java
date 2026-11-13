
package catalogo_digital_backend.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(
                min = 2,
                max = 150,
                message = "El nombre debe tener entre 2 y 150 caracteres"
        )
        String name,

        @Size(
                max = 2000,
                message = "La descripción no puede superar 2000 caracteres"
        )
        String description,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(
                value = "0.00",
                inclusive = true,
                message = "El precio no puede ser negativo"
        )
        @Digits(
                integer = 10,
                fraction = 2,
                message = "El precio debe tener máximo 10 dígitos enteros y 2 decimales"
        )
        BigDecimal price,

        @NotNull(message = "La categoría es obligatoria")
        UUID categoryId

) {
}
