
package catalogo_digital_backend.service;

import catalogo_digital_backend.entity.ImageDeletionJob;
import catalogo_digital_backend.repository.ImageDeletionJobRepository;
import catalogo_digital_backend.storage.CloudinaryImageStorageService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.transaction.TransactionStatus;
import java.util.function.Consumer;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ImageDeletionWorkerTest {

    private ImageDeletionJobRepository jobRepository;
    private ProductImageService imageService;
    private CloudinaryImageStorageService storage;
    private TransactionTemplate transactionTemplate;

    private ImageDeletionWorker worker;

    private final UUID productId = UUID.randomUUID();
    private final UUID imageId = UUID.randomUUID();

    private ImageDeletionJob job;

    @BeforeEach
    void setUp() {

        jobRepository = mock(ImageDeletionJobRepository.class);
        imageService = mock(ProductImageService.class);
        storage = mock(CloudinaryImageStorageService.class);
        transactionTemplate = mock(TransactionTemplate.class);

        job = new ImageDeletionJob(
                productId,
                imageId,
                "catalogo-digital/products/test"
        );

        // Asignar un UUID al trabajo simulado.
        // En producción lo asigna Hibernate.
        // Aquí utilizamos reflexión solo para la prueba.
        try {
            var field = ImageDeletionJob.class
                    .getDeclaredField("id");

            field.setAccessible(true);
            field.set(job, UUID.randomUUID());

        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }

        when(jobRepository.existsById(job.getId()))
                .thenReturn(true);

        when(jobRepository.findById(job.getId()))
                .thenReturn(Optional.of(job));

        
doAnswer(invocation -> {

    @SuppressWarnings("unchecked")
    Consumer<TransactionStatus> callback =
            invocation.getArgument(0);

    callback.accept(
            mock(TransactionStatus.class)
    );

    return null;

}).when(transactionTemplate)
        .executeWithoutResult(any());


        worker = new ImageDeletionWorker(
                jobRepository,
                imageService,
                storage,
                transactionTemplate
        );
    }

    @Test
    void shouldDeleteImageAndJob() {

        worker.processJob(job);

        verify(storage).delete(
                job.getCloudinaryPublicId()
        );

        verify(imageService).deleteImage(
                productId,
                imageId
        );

        verify(jobRepository).deleteById(job.getId());
    }

    @Test
    void shouldScheduleRetryWhenCloudinaryFails() {

        doThrow(new IllegalStateException("Fallo simulado"))
                .when(storage)
                .delete(job.getCloudinaryPublicId());

        worker.processJob(job);

        assertEquals(1, job.getAttempts());
        assertNotNull(job.getLastError());

        verify(imageService, never())
                .deleteImage(any(), any());

        verify(jobRepository).save(job);
    }
}
