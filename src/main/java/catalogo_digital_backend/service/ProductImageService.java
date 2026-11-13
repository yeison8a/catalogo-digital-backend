
package catalogo_digital_backend.service;

import catalogo_digital_backend.dto.product.image.ProductImageRequest;
import catalogo_digital_backend.dto.product.image.ProductImageResponse;
import catalogo_digital_backend.entity.Product;
import catalogo_digital_backend.entity.ProductImage;
import catalogo_digital_backend.repository.ProductImageRepository;
import catalogo_digital_backend.repository.ProductRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductImageService {

    private static final int MAX_IMAGES = 5;

    private final ProductImageRepository imageRepository;
    private final ProductRepository productRepository;

    public ProductImageService(
            ProductImageRepository imageRepository,
            ProductRepository productRepository
    ) {
        this.imageRepository = imageRepository;
        this.productRepository = productRepository;
    }

    // 1. Consultar las imágenes de un producto
    @Transactional(readOnly = true)
    public List<ProductImageResponse> findByProduct(
            UUID productId
    ) {
        getProductOrThrow(productId);

        return imageRepository
                .findByProduct_IdOrderBySortOrderAsc(productId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // 2. Agregar una imagen
    @Transactional
    public ProductImageResponse addImage(
            UUID productId,
            ProductImageRequest request
    ) {
        Product product = lockProductOrThrow(productId);
        String imageUrl = validateImageUrl(request);

        List<ProductImage> existingImages = imageRepository
                .findByProduct_IdOrderBySortOrderAsc(productId);

        if (existingImages.size() >= MAX_IMAGES) {
            throw new IllegalStateException(
                    "Un producto no puede tener más de 5 imágenes."
            );
        }

        // La primera imagen es principal.
        boolean isMain = existingImages.isEmpty();

        // Las nuevas imágenes se agregan al final.
        int sortOrder = existingImages.size();

        ProductImage image = new ProductImage(
                imageUrl,
                sortOrder,
                isMain,
                product
        );

        ProductImage saved =
                imageRepository.saveAndFlush(image);

        return toResponse(saved);
    }


// Registrar una imagen que ya fue subida a Cloudinary
@Transactional
public ProductImageResponse addUploadedImage(
        UUID productId,
        String imageUrl,
        String cloudinaryPublicId
) {
    if (cloudinaryPublicId == null
            || cloudinaryPublicId.isBlank()) {
        throw new IllegalArgumentException(
                "El identificador de Cloudinary es obligatorio."
        );
    }

    // Reutilizar la validación de URL existente.
    String validUrl = validateImageUrl(
            new ProductImageRequest(imageUrl)
    );

    Product product = lockProductOrThrow(productId);

    List<ProductImage> existingImages = imageRepository
            .findByProduct_IdOrderBySortOrderAsc(productId);

    if (existingImages.size() >= MAX_IMAGES) {
        throw new IllegalStateException(
                "Un producto no puede tener más de 5 imágenes."
        );
    }

    boolean isMain = existingImages.isEmpty();
    int sortOrder = existingImages.size();

    ProductImage image = new ProductImage(
            validUrl,
            sortOrder,
            isMain,
            product
    );

    image.setCloudinaryPublicId(
            cloudinaryPublicId
    );

    ProductImage saved =
            imageRepository.saveAndFlush(image);

    return toResponse(saved);
}


    // 3. Cambiar la imagen principal

    @Transactional
    public ProductImageResponse setMainImage(
            UUID productId,
            UUID imageId
    ) {

        // 1. Bloquear el producto.
        lockProductOrThrow(productId);

        // 2. Recuperar sus fotografías.
        List<ProductImage> images = imageRepository
                .findByProduct_IdOrderBySortOrderAsc(productId);

        // 3. Comprobar que la fotografía seleccionada
        // realmente pertenece a este producto.
        ProductImage selected = images.stream()
                .filter(image -> image.getId().equals(imageId))
                .findFirst()
                .orElseThrow(
                        () -> new NoSuchElementException(
                                "La imagen no pertenece al producto."
                        )
                );

        // 4. Si ya es la única principal, no cambiar nada.
        long currentMainCount = images.stream()
                .filter(ProductImage::isMainImage)
                .count();

        if (selected.isMainImage() && currentMainCount == 1) {
            return toResponse(selected);
        }

        // 5. Desmarcar las imágenes principales anteriores.
        for (ProductImage image : images) {
            image.setMainImage(false);
        }

        // Confirmar las actualizaciones SQL dentro
        // de la transacción actual.
        imageRepository.saveAllAndFlush(images);

        // 6. Marcar la nueva principal.
        selected.setMainImage(true);

        imageRepository.saveAndFlush(selected);

        return toResponse(selected);
    }


    // 4. Cambiar el orden de las imágenes
    @Transactional
    public List<ProductImageResponse> reorderImages(
            UUID productId,
            List<UUID> orderedImageIds
    ) {
        lockProductOrThrow(productId);

        List<ProductImage> images = imageRepository
                .findByProduct_IdOrderBySortOrderAsc(productId);

        if (orderedImageIds == null
                || orderedImageIds.size() != images.size()) {
            throw new IllegalArgumentException(
                    "Debes enviar todas las imágenes del producto."
            );
        }

        Set<UUID> existingIds = new HashSet<>();

        for (ProductImage image : images) {
            existingIds.add(image.getId());
        }

        Set<UUID> receivedIds =
                new HashSet<>(orderedImageIds);

        // Evita IDs duplicados, faltantes o de otro producto.
        if (receivedIds.size() != orderedImageIds.size()
                || !receivedIds.equals(existingIds)) {
            throw new IllegalArgumentException(
                    "La lista contiene imágenes inválidas o repetidas."
            );
        }

        for (int index = 0; index < orderedImageIds.size(); index++) {

            UUID currentId = orderedImageIds.get(index);

            ProductImage image = images.stream()
                    .filter(item -> item.getId().equals(currentId))
                    .findFirst()
                    .orElseThrow();

            image.setSortOrder(index);
        }

        imageRepository.saveAllAndFlush(images);

        return findByProduct(productId);
    }

    // 5. Eliminar una imagen
    @Transactional
    public void deleteImage(
            UUID productId,
            UUID imageId
    ) {
        lockProductOrThrow(productId);

        ProductImage image = getImageOrThrow(
                productId,
                imageId
        );

        boolean wasMain = image.isMainImage();

        imageRepository.delete(image);
        imageRepository.flush();

        List<ProductImage> remainingImages =
                new ArrayList<>(
                        imageRepository
                                .findByProduct_IdOrderBySortOrderAsc(
                                        productId
                                )
                );

        // Reorganizar posiciones: 0, 1, 2...
        for (int index = 0; index < remainingImages.size(); index++) {
            remainingImages.get(index).setSortOrder(index);
        }

        // Si eliminamos la principal, seleccionar otra.
        if (wasMain && !remainingImages.isEmpty()) {
            for (ProductImage remaining : remainingImages) {
                remaining.setMainImage(false);
            }

            remainingImages.get(0).setMainImage(true);
        }

        imageRepository.saveAllAndFlush(remainingImages);
    }

    // Buscar el producto
    private Product getProductOrThrow(UUID productId) {

        if (productId == null) {
            throw new IllegalArgumentException(
                    "El identificador del producto es obligatorio."
            );
        }

        return productRepository.findById(productId)
                .orElseThrow(
                        () -> new NoSuchElementException(
                                "No existe un producto con ID: " + productId
                        )
                );
    }


    private Product lockProductOrThrow(UUID productId) {

        if (productId == null) {
            throw new IllegalArgumentException(
                    "El identificador del producto es obligatorio."
            );
        }

        return productRepository
                .findByIdForUpdate(productId)
                .orElseThrow(
                        () -> new NoSuchElementException(
                                "No existe un producto con ID: " + productId
                        )
                );
    }


    // Buscar una imagen y comprobar su propietario
    private ProductImage getImageOrThrow(
            UUID productId,
            UUID imageId
    ) {
        if (imageId == null) {
            throw new IllegalArgumentException(
                    "El identificador de la imagen es obligatorio."
            );
        }

        ProductImage image = imageRepository.findById(imageId)
                .orElseThrow(
                        () -> new NoSuchElementException(
                                "No existe la imagen solicitada."
                        )
                );

        if (!image.getProduct().getId().equals(productId)) {
            throw new NoSuchElementException(
                    "La imagen no pertenece al producto."
            );
        }

        return image;
    }

    // Validar URL
    private String validateImageUrl(ProductImageRequest request) {

        if (request == null || request.imageUrl() == null) {
            throw new IllegalArgumentException(
                    "La URL de la imagen es obligatoria."
            );
        }

        String url = request.imageUrl().trim();

        if (url.isEmpty() || url.length() > 2048) {
            throw new IllegalArgumentException(
                    "La URL debe tener entre 1 y 2048 caracteres."
            );
        }

        try {
            URI uri = URI.create(url);

            String scheme = uri.getScheme();

            boolean validScheme =
                    "https".equalsIgnoreCase(scheme)
                    || "http".equalsIgnoreCase(scheme);

            if (!validScheme
                    || uri.getHost() == null
                    || uri.getUserInfo() != null) {
                throw new IllegalArgumentException(
                        "Debes proporcionar una URL HTTP o HTTPS válida."
                );
            }

        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Debes proporcionar una URL HTTP o HTTPS válida."
            );
        }

        return url;
    }

    // Convertir entidad a DTO
    private ProductImageResponse toResponse(
            ProductImage image
    ) {
        return new ProductImageResponse(
                image.getId(),
                image.getImageUrl(),
                image.getSortOrder(),
                image.isMainImage(),
                image.getProduct().getId()
        );
    }
}
