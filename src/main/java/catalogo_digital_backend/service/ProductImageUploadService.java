
package catalogo_digital_backend.service;

import catalogo_digital_backend.dto.product.image.ProductImageResponse;
import catalogo_digital_backend.storage.CloudinaryImageStorageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@Profile("local")
public class ProductImageUploadService {

    private static final Logger logger =
            LoggerFactory.getLogger(ProductImageUploadService.class);

    private static final int MAX_IMAGES = 5;

    private final CloudinaryImageStorageService cloudinaryStorage;
    private final ProductImageService imageService;
    private final ProductService productService;
    private final TransactionTemplate transactionTemplate;

    public ProductImageUploadService(
            CloudinaryImageStorageService cloudinaryStorage,
            ProductImageService imageService,
            ProductService productService,
            TransactionTemplate transactionTemplate
    ) {
        this.cloudinaryStorage = cloudinaryStorage;
        this.imageService = imageService;
        this.productService = productService;
        this.transactionTemplate = transactionTemplate;
    }

    public ProductImageResponse upload(
            UUID productId,
            MultipartFile file
    ) {

        // Verificar que el producto exista.
        productService.findById(productId);

        // Comprobar el límite de fotografías.
        if (imageService.findByProduct(productId).size()
                >= MAX_IMAGES) {
            throw new IllegalStateException(
                    "Un producto no puede tener más de 5 imágenes."
            );
        }

        // Subir la fotografía a Cloudinary.
        CloudinaryImageStorageService.StoredImage stored =
                cloudinaryStorage.store(file);

        try {

            // Registrar la fotografía en PostgreSQL.
            ProductImageResponse response =
                    transactionTemplate.execute(status ->
                            imageService.addUploadedImage(
                                    productId,
                                    stored.secureUrl(),
                                    stored.publicId()
                            )
                    );

            if (response == null) {
                throw new IllegalStateException(
                        "No se pudo registrar la imagen."
                );
            }

            return response;

        } catch (RuntimeException databaseException) {

            // Intentar eliminar el archivo si falla PostgreSQL.
            try {
                cloudinaryStorage.delete(stored.publicId());

            } catch (RuntimeException cleanupException) {

                logger.error(
                        "No se pudo eliminar la imagen de Cloudinary: {}",
                        stored.publicId(),
                        cleanupException
                );

                databaseException.addSuppressed(
                        cleanupException
                );
            }

            throw databaseException;
        }
    }
}
