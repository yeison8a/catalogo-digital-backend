
package catalogo_digital_backend.service;

import catalogo_digital_backend.dto.product.image.ProductImageResponse;
import catalogo_digital_backend.storage.CloudinaryImageStorageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProductImageUploadServiceTest {

    private CloudinaryImageStorageService storage;
    private ProductImageService imageService;
    private ProductService productService;
    private TransactionTemplate transactionTemplate;

    private ProductImageUploadService uploadService;

    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {

        storage = mock(CloudinaryImageStorageService.class);
        imageService = mock(ProductImageService.class);
        productService = mock(ProductService.class);
        transactionTemplate = mock(TransactionTemplate.class);

        // Ejecutar el callback simulado de transacción.
        when(transactionTemplate.execute(
                any(TransactionCallback.class)
        )).thenAnswer(invocation -> {

            TransactionCallback<?> callback =
                    invocation.getArgument(0);

            return callback.doInTransaction(
                    mock(TransactionStatus.class)
            );
        });

        uploadService = new ProductImageUploadService(
                storage,
                imageService,
                productService,
                transactionTemplate
        );
    }

    private MockMultipartFile createFile() {
        return new MockMultipartFile(
                "file",
                "producto.png",
                "image/png",
                new byte[]{1, 2, 3}
        );
    }

    private CloudinaryImageStorageService.StoredImage stored() {
        return new CloudinaryImageStorageService.StoredImage(
                "catalogo-digital/products/test-image",
                "https://res.cloudinary.com/demo/image/upload/test.png",
                "image/png",
                3
        );
    }

    @Test
    void shouldUploadAndRegisterImage() {

        when(storage.store(any())).thenReturn(stored());

        ProductImageResponse expected =
                new ProductImageResponse(
                        UUID.randomUUID(),
                        stored().secureUrl(),
                        0,
                        true,
                        productId
                );

        when(imageService.addUploadedImage(
                eq(productId),
                eq(stored().secureUrl()),
                eq(stored().publicId())
        )).thenReturn(expected);

        ProductImageResponse result =
                uploadService.upload(
                        productId,
                        createFile()
                );

        assertEquals(expected.id(), result.id());

        verify(storage).store(any());
        verify(imageService).addUploadedImage(
                productId,
                stored().secureUrl(),
                stored().publicId()
        );

        verify(storage, never()).delete(anyString());
    }

    @Test
    void shouldDeleteCloudinaryImageWhenDatabaseFails() {

        when(storage.store(any())).thenReturn(stored());

        when(imageService.addUploadedImage(
                any(),
                anyString(),
                anyString()
        )).thenThrow(
                new IllegalStateException(
                        "Fallo al registrar en PostgreSQL"
                )
        );

        assertThrows(
                IllegalStateException.class,
                () -> uploadService.upload(
                        productId,
                        createFile()
                )
        );

        verify(storage).delete(
                stored().publicId()
        );
    }

    @Test
    void shouldNotUploadWhenProductDoesNotExist() {

        when(productService.findById(productId))
                .thenThrow(
                        new java.util.NoSuchElementException(
                                "Producto inexistente"
                        )
                );

        assertThrows(
                java.util.NoSuchElementException.class,
                () -> uploadService.upload(
                        productId,
                        createFile()
                )
        );

        verify(storage, never()).store(any());
    }
}
