
package catalogo_digital_backend.controller;

import catalogo_digital_backend.dto.product.image.ProductImageResponse;
import catalogo_digital_backend.service.ProductImageService;
import catalogo_digital_backend.service.ProductService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products/{productId}/images")
public class ProductImageController {

    private final ProductImageService imageService;
    private final ProductService productService;

    public ProductImageController(
            ProductImageService imageService,
            ProductService productService
    ) {
        this.imageService = imageService;
        this.productService = productService;
    }

    // Consultar imágenes de un producto público
    @GetMapping
    public List<ProductImageResponse> findPublicImages(
            @PathVariable UUID productId
    ) {

        // Impedir consultar imágenes de productos ocultos
        // o pertenecientes a categorías inactivas.
        productService.findPublishedById(productId);

        return imageService.findByProduct(productId);
    }
}
