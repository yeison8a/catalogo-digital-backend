
package catalogo_digital_backend.controller;

import catalogo_digital_backend.dto.product.ProductResponse;
import catalogo_digital_backend.service.ProductService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(
            ProductService productService
    ) {
        this.productService = productService;
    }

    // Consultar todos los productos públicos
    @GetMapping
    public List<ProductResponse> findPublished() {

        return productService.findPublished();
    }

    // Consultar un producto público por UUID
    @GetMapping("/{id}")
    public ProductResponse findById(
            @PathVariable UUID id
    ) {

        return productService.findPublishedById(id);
    }
}
