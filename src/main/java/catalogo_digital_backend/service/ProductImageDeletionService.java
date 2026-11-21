
package catalogo_digital_backend.service;

import catalogo_digital_backend.entity.ImageDeletionJob;
import catalogo_digital_backend.entity.ProductImage;

import catalogo_digital_backend.repository.ImageDeletionJobRepository;
import catalogo_digital_backend.repository.ProductImageRepository;
import catalogo_digital_backend.repository.ProductRepository;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Profile("local")
public class ProductImageDeletionService {

    private final ProductRepository productRepository;
    private final ProductImageRepository imageRepository;
    private final ImageDeletionJobRepository jobRepository;

    public ProductImageDeletionService(
            ProductRepository productRepository,
            ProductImageRepository imageRepository,
            ImageDeletionJobRepository jobRepository
    ) {
        this.productRepository = productRepository;
        this.imageRepository = imageRepository;
        this.jobRepository = jobRepository;
    }

    @Transactional
    public void requestDeletion(
            UUID productId,
            UUID imageId
    ) {

        if (productId == null || imageId == null) {
            throw new IllegalArgumentException(
                    "Los identificadores son obligatorios."
            );
        }

        // Bloquear el producto para coordinar
        // cambios de la galería.
        productRepository.findByIdForUpdate(productId)
                .orElseThrow(
                        () -> new NoSuchElementException(
                                "No existe el producto."
                        )
                );

        ProductImage image = imageRepository
                .findById(imageId)
                .orElseThrow(
                        () -> new NoSuchElementException(
                                "No existe la imagen."
                        )
                );

        if (!image.getProduct().getId().equals(productId)) {
            throw new NoSuchElementException(
                    "La imagen no pertenece al producto."
            );
        }

        // Solicitud repetida: no crear otro trabajo.
        if (image.isDeletionPending()) {
            return;
        }

        if (jobRepository.existsByImageId(imageId)) {
            throw new IllegalStateException(
                    "Ya existe un trabajo de eliminación."
            );
        }

        // Marcar la fotografía.
        image.setDeletionPending(true);
        imageRepository.save(image);

        // Crear el trabajo en la misma transacción.
        ImageDeletionJob job = new ImageDeletionJob(
                productId,
                imageId,
                image.getCloudinaryPublicId()
        );

        jobRepository.save(job);
    }
}
