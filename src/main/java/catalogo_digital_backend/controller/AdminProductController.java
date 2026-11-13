
package catalogo_digital_backend.controller;

import catalogo_digital_backend.dto.product.ProductRequest;
import catalogo_digital_backend.dto.product.ProductResponse;
import catalogo_digital_backend.dto.product.ProductAvailabilityRequest;
import catalogo_digital_backend.dto.product.ProductPublicationRequest;

import catalogo_digital_backend.service.ProductService;

import jakarta.validation.Valid;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@Profile("local")
@RequestMapping("/api/v1/admin/products")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(
            ProductService productService
    ) {
        this.productService = productService;
    }

    // 1. Consultar todos los productos
    @GetMapping
    public List<ProductResponse> findAll() {
        return productService.findAll();
    }

    // 2. Consultar un producto por su ID
    @GetMapping("/{id}")
    public ProductResponse findById(
            @PathVariable UUID id
    ) {
        return productService.findById(id);
    }

    // 3. Crear un producto
    @PostMapping
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody ProductRequest request
    ) {
        ProductResponse created =
                productService.create(request);

        URI location = URI.create(
                "/api/v1/admin/products/" + created.id()
        );

        return ResponseEntity
                .created(location)
                .body(created);
    }

    // 4. Actualizar información de un producto
    @PutMapping("/{id}")
    public ProductResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequest request
    ) {
        return productService.update(id, request);
    }

    // 5. Cambiar disponibilidad
    @PatchMapping("/{id}/availability")
    public ProductResponse updateAvailability(
            @PathVariable UUID id,
            @Valid @RequestBody
                    ProductAvailabilityRequest request
    ) {
        return productService.updateAvailability(
                id,
                request.available()
        );
    }

    // 6. Publicar u ocultar un producto
    @PatchMapping("/{id}/publication")
    public ProductResponse updatePublication(
            @PathVariable UUID id,
            @Valid @RequestBody
                    ProductPublicationRequest request
    ) {
        return productService.updatePublication(
                id,
                request.active()
        );
    }
}
