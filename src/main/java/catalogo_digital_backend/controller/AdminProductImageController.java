
package catalogo_digital_backend.controller;

import catalogo_digital_backend.dto.product.image.ProductImageRequest;
import catalogo_digital_backend.dto.product.image.ProductImageResponse;
import catalogo_digital_backend.dto.product.image.ProductImageOrderRequest;

import catalogo_digital_backend.service.ProductImageService;

import jakarta.validation.Valid;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import catalogo_digital_backend.service.ProductImageUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;


import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@Profile("local")
@RequestMapping("/api/v1/admin/products/{productId}/images")
public class AdminProductImageController {

    private final ProductImageService imageService;
    private final ProductImageUploadService uploadService;

    public AdminProductImageController(
            ProductImageService imageService,
            ProductImageUploadService uploadService
    ) {
        this.imageService = imageService;
        this.uploadService = uploadService;
    }


    // Subir una fotografía real a Cloudinary
    @PostMapping(
            value = "/upload",
            consumes = "multipart/form-data"
    )
    public ResponseEntity<ProductImageResponse> uploadImage(
            @PathVariable UUID productId,
            @RequestParam("file") MultipartFile file
    ) {

        ProductImageResponse created =
                uploadService.upload(productId, file);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }


    // 1. Consultar todas las imágenes del producto
    @GetMapping
    public List<ProductImageResponse> findAll(
            @PathVariable UUID productId
    ) {
        return imageService.findByProduct(productId);
    }

    // 2. Agregar una imagen al producto
    @PostMapping
    public ResponseEntity<ProductImageResponse> addImage(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductImageRequest request
    ) {

        ProductImageResponse created =
                imageService.addImage(productId, request);

        URI location = URI.create(
                "/api/v1/admin/products/"
                        + productId
                        + "/images/"
                        + created.id()
        );

        return ResponseEntity
                .created(location)
                .body(created);
    }

    // 3. Establecer la imagen principal
    @PutMapping("/{imageId}/main")
    public ProductImageResponse setMainImage(
            @PathVariable UUID productId,
            @PathVariable UUID imageId
    ) {

        return imageService.setMainImage(
                productId,
                imageId
        );
    }

    // 4. Cambiar el orden de las imágenes
    @PutMapping("/order")
    public List<ProductImageResponse> reorderImages(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductImageOrderRequest request
    ) {

        return imageService.reorderImages(
                productId,
                request.imageIds()
        );
    }

    // 5. Eliminar una imagen
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable UUID productId,
            @PathVariable UUID imageId
    ) {

        imageService.deleteImage(productId, imageId);

        return ResponseEntity.noContent().build();
    }
}
