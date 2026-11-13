
package catalogo_digital_backend.service;

import catalogo_digital_backend.entity.ProductImage;
import catalogo_digital_backend.exception.CloudinaryDeletionException;
import catalogo_digital_backend.repository.ProductImageRepository;
import catalogo_digital_backend.storage.CloudinaryImageStorageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Profile("local")
public class ProductImageDeletionService {

    private static final Logger logger =
            LoggerFactory.getLogger(ProductImageDeletionService.class);

    private final ProductImageRepository imageRepository;
    private final ProductImageService imageService;
    private final CloudinaryImageStorageService cloudinaryStorage;
    private final TransactionTemplate transactionTemplate;

    public ProductImageDeletionService(
            ProductImageRepository imageRepository,
            ProductImageService imageService,
            CloudinaryImageStorageService cloudinaryStorage,
            TransactionTemplate transactionTemplate
    ) {
        this.imageRepository = imageRepository;
        this.imageService = imageService;
        this.cloudinaryStorage = cloudinaryStorage;
        this.transactionTemplate = transactionTemplate;
    }

    public void delete(
            UUID productId,
            UUID imageId
    ) {

        // 1. Comprobar identificadores
        if (productId == null || imageId == null) {
            throw new IllegalArgumentException(
                    "Los identificadores son obligatorios."
            );
        }

        // 2. Buscar la fotografía
        ProductImage image = imageRepository
                .findById(imageId)
                .orElseThrow(
                        () -> new NoSuchElementException(
                                "No existe la imagen solicitada."
                        )
                );

        // 3. Comprobar que pertenece al producto
        if (!image.getProduct().getId().equals(productId)) {
            throw new NoSuchElementException(
                    "La imagen no pertenece al producto."
            );
        }

        String publicId = image.getCloudinaryPublicId();

        // 4. Eliminar el archivo remoto si existe
        if (publicId != null && !publicId.isBlank()) {

            try {
                cloudinaryStorage.delete(publicId);

            } catch (RuntimeException exception) {

                logger.error(
                        "No fue posible eliminar el recurso de Cloudinary: {}",
                        publicId,
                        exception
                );

                throw new CloudinaryDeletionException(
                        "Falló la eliminación del archivo remoto.",
                        exception
                );
            }
        }

        // 5. Eliminar el registro mediante una
        // transacción independiente y completar
        // las reglas existentes de la galería.
        try {

            transactionTemplate.executeWithoutResult(
                    status -> imageService.deleteImage(
                            productId,
                            imageId
                    )
            );

        } catch (RuntimeException exception) {

            logger.error(
                    "Falló la eliminación en PostgreSQL. "
                            + "Puede requerirse reconciliación. "
                            + "productId={}, imageId={}, publicId={}",
                    productId,
                    imageId,
                    publicId,
                    exception
            );

            throw exception;
        }
    }
}
