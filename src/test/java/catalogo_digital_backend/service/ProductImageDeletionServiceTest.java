
package catalogo_digital_backend.service;

import catalogo_digital_backend.entity.Product;
import catalogo_digital_backend.entity.ProductImage;
import catalogo_digital_backend.exception.CloudinaryDeletionException;
import catalogo_digital_backend.repository.ProductImageRepository;
import catalogo_digital_backend.storage.CloudinaryImageStorageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProductImageDeletionServiceTest {

    private ProductImageRepository imageRepository;
    private ProductImageService imageService;
    private CloudinaryImageStorageService cloudinaryStorage;
    private TransactionTemplate transactionTemplate;

    private ProductImageDeletionService deletionService;

    private final UUID productId = UUID.randomUUID();
    private final UUID imageId = UUID.randomUUID();

    private static final String PUBLIC_ID =
            "catalogo-digital/products/test-image";

    @BeforeEach
    void setUp() {

        imageRepository = mock(ProductImageRepository.class);
        imageService = mock(ProductImageService.class);
        cloudinaryStorage =
                mock(CloudinaryImageStorageService.class);

        transactionTemplate = mock(TransactionTemplate.class);

        // Simular la ejecución del bloque transaccional
        doAnswer(invocation -> {

            Consumer<TransactionStatus> callback =
                    invocation.getArgument(0);

            callback.accept(mock(TransactionStatus.class));

            return null;

        }).when(transactionTemplate)
                .executeWithoutResult(any());

        deletionService = new ProductImageDeletionService(
                imageRepository,
                imageService,
                cloudinaryStorage,
                transactionTemplate
        );
    }

    private ProductImage mockImage(String publicId) {

        Product product = mock(Product.class);
        when(product.getId()).thenReturn(productId);

        ProductImage image = mock(ProductImage.class);

        when(image.getProduct()).thenReturn(product);
        when(image.getCloudinaryPublicId()).thenReturn(publicId);

        when(imageRepository.findById(imageId))
                .thenReturn(Optional.of(image));

        return image;
    }

    @Test
    void shouldDeleteFromCloudinaryAndDatabase() {

        mockImage(PUBLIC_ID);

        deletionService.delete(productId, imageId);

        verify(cloudinaryStorage).delete(PUBLIC_ID);

        verify(imageService).deleteImage(
                productId,
                imageId
        );
    }

    @Test
    void shouldKeepDatabaseRecordWhenCloudinaryFails() {

        mockImage(PUBLIC_ID);

        doThrow(
                new IllegalStateException(
                        "Cloudinary no está disponible"
                )
        ).when(cloudinaryStorage).delete(PUBLIC_ID);

        assertThrows(
                CloudinaryDeletionException.class,
                () -> deletionService.delete(
                        productId,
                        imageId
                )
        );

        verify(imageService, never()).deleteImage(
                any(),
                any()
        );
    }

    @Test
    void shouldDeleteLegacyImageWithoutCloudinaryCall() {

        // Fotografía registrada mediante una URL antigua
        mockImage(null);

        deletionService.delete(productId, imageId);

        verifyNoInteractions(cloudinaryStorage);

        verify(imageService).deleteImage(
                productId,
                imageId
        );
    }

    @Test
    void shouldRejectImageFromAnotherProduct() {

        ProductImage image = mock(ProductImage.class);
        Product otherProduct = mock(Product.class);

        when(otherProduct.getId())
                .thenReturn(UUID.randomUUID());

        when(image.getProduct()).thenReturn(otherProduct);

        when(imageRepository.findById(imageId))
                .thenReturn(Optional.of(image));

        assertThrows(
                java.util.NoSuchElementException.class,
                () -> deletionService.delete(
                        productId,
                        imageId
                )
        );

        verifyNoInteractions(cloudinaryStorage);
        verify(imageService, never())
                .deleteImage(any(), any());
    }
}
