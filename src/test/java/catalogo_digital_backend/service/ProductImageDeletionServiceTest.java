
package catalogo_digital_backend.service;

import catalogo_digital_backend.entity.ImageDeletionJob;
import catalogo_digital_backend.entity.Product;
import catalogo_digital_backend.entity.ProductImage;

import catalogo_digital_backend.repository.ImageDeletionJobRepository;
import catalogo_digital_backend.repository.ProductImageRepository;
import catalogo_digital_backend.repository.ProductRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProductImageDeletionServiceTest {

    private ProductRepository productRepository;
    private ProductImageRepository imageRepository;
    private ImageDeletionJobRepository jobRepository;
    private ProductImageDeletionService deletionService;

    private final UUID productId = UUID.randomUUID();
    private final UUID imageId = UUID.randomUUID();

    private ProductImage image;

    @BeforeEach
    void setUp() {

        productRepository = mock(ProductRepository.class);
        imageRepository = mock(ProductImageRepository.class);
        jobRepository = mock(ImageDeletionJobRepository.class);

        deletionService = new ProductImageDeletionService(
                productRepository,
                imageRepository,
                jobRepository
        );

        Product product = mock(Product.class);
        when(product.getId()).thenReturn(productId);

        image = mock(ProductImage.class);

        when(image.getProduct()).thenReturn(product);

        when(productRepository.findByIdForUpdate(productId))
                .thenReturn(Optional.of(product));

        when(imageRepository.findById(imageId))
                .thenReturn(Optional.of(image));
    }

    @Test
    void shouldCreateDeletionJob() {

        when(image.isDeletionPending()).thenReturn(false);

        when(image.getCloudinaryPublicId())
                .thenReturn("catalogo-digital/products/test");

        deletionService.requestDeletion(
                productId,
                imageId
        );

        verify(image).setDeletionPending(true);

        verify(imageRepository).save(image);

        verify(jobRepository).save(
                any(ImageDeletionJob.class)
        );
    }

    @Test
    void shouldNotCreateDuplicateJob() {

        when(image.isDeletionPending()).thenReturn(true);

        deletionService.requestDeletion(
                productId,
                imageId
        );

        verify(jobRepository, never())
                .save(any(ImageDeletionJob.class));
    }

    @Test
    void shouldRejectUnknownProduct() {

        when(productRepository.findByIdForUpdate(productId))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> deletionService.requestDeletion(
                        productId,
                        imageId
                )
        );

        verify(jobRepository, never())
                .save(any(ImageDeletionJob.class));
    }
}
