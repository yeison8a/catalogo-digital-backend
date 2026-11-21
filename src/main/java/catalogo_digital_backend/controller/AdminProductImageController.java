
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

import catalogo_digital_backend.service.ProductImageDeletionService;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(
    name = "Administración de fotografías",
    description = "Subida a Cloudinary, orden, imagen principal y eliminaciones"
)
@RestController
@Profile("local")
@RequestMapping("/api/v1/admin/products/{productId}/images")
public class AdminProductImageController {

    private final ProductImageService imageService;
    private final ProductImageUploadService uploadService;
    private final ProductImageDeletionService deletionService;

    public AdminProductImageController(
            ProductImageService imageService,
            ProductImageUploadService uploadService,
            ProductImageDeletionService deletionService
    ) {
        this.imageService = imageService;
        this.uploadService = uploadService;
        this.deletionService = deletionService;
    }


@Operation(
    summary = "Subir una fotografía a Cloudinary",
    description = """
            Recibe una imagen JPEG o PNG mediante multipart/form-data.

            Reglas:
            - Tamaño máximo: 5 MiB.
            - Máximo cinco fotografías por producto.
            - La primera fotografía será la principal.
            - Se almacena el archivo en Cloudinary.
            - Se guarda su referencia en PostgreSQL.
            """
)
@ApiResponse(
    responseCode = "201",
    description = "Fotografía almacenada correctamente"
)
@ApiResponse(
    responseCode = "400",
    description = "Archivo inválido"
)
@ApiResponse(
    responseCode = "404",
    description = "Producto inexistente"
)
@ApiResponse(
    responseCode = "409",
    description = "El producto ya tiene cinco fotografías"
)
@ApiResponse(
    responseCode = "413",
    description = "El archivo supera el tamaño permitido"
)

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


@Operation(
    summary = "Solicitar eliminación de fotografía",
    description = """
            Registra una solicitud de eliminación.

            La fotografía se marca como pendiente y un proceso
            automático intenta eliminarla de Cloudinary y PostgreSQL.

            La respuesta 202 indica que la solicitud fue aceptada,
            no que el archivo ya fue eliminado.
            """
)
@ApiResponse(
    responseCode = "202",
    description = "Solicitud de eliminación registrada"
)
@ApiResponse(
    responseCode = "404",
    description = "Producto o fotografía inexistente"
)

    // 5. Eliminar una imagen
@DeleteMapping("/{imageId}")
public ResponseEntity<Void> deleteImage(
        @PathVariable UUID productId,
        @PathVariable UUID imageId
) {

    deletionService.requestDeletion(
            productId,
            imageId
    );

    return ResponseEntity
            .accepted()
            .build();
}

}
