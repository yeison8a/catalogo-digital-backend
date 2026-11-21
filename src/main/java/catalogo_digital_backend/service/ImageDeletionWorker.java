
package catalogo_digital_backend.service;

import catalogo_digital_backend.entity.ImageDeletionJob;
import catalogo_digital_backend.repository.ImageDeletionJobRepository;
import catalogo_digital_backend.storage.CloudinaryImageStorageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Profile("local")
@ConditionalOnProperty(
        name = "app.image-deletion.enabled",
        havingValue = "true"
)
public class ImageDeletionWorker {

    private static final Logger logger =
            LoggerFactory.getLogger(ImageDeletionWorker.class);

    private final ImageDeletionJobRepository jobRepository;
    private final ProductImageService imageService;
    private final CloudinaryImageStorageService cloudinaryStorage;
    private final TransactionTemplate transactionTemplate;

    public ImageDeletionWorker(
            ImageDeletionJobRepository jobRepository,
            ProductImageService imageService,
            CloudinaryImageStorageService cloudinaryStorage,
            TransactionTemplate transactionTemplate
    ) {
        this.jobRepository = jobRepository;
        this.imageService = imageService;
        this.cloudinaryStorage = cloudinaryStorage;
        this.transactionTemplate = transactionTemplate;
    }

    // Buscar trabajos pendientes periódicamente.
    @Scheduled(
            fixedDelayString =
                    "${app.image-deletion.poll-ms:15000}"
    )
    public void processPendingJobs() {

        List<ImageDeletionJob> jobs = jobRepository
                .findTop20ByNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                        Instant.now()
                );

        for (ImageDeletionJob job : jobs) {
            processJob(job);
        }
    }

    // Procesar un trabajo individual.
    public void processJob(ImageDeletionJob job) {

        UUID jobId = job.getId();
        String publicId = job.getCloudinaryPublicId();

        try {

            // Paso 1: eliminar el recurso remoto.
            // Las imágenes antiguas no tienen publicId.
            if (publicId != null && !publicId.isBlank()) {
                cloudinaryStorage.delete(publicId);
            }

            // Paso 2: confirmar la eliminación local.
            // La imagen y el trabajo se eliminan
            // dentro de la misma transacción.
            transactionTemplate.executeWithoutResult(status -> {

                if (!jobRepository.existsById(jobId)) {
                    return;
                }

                imageService.deleteImage(
                        job.getProductId(),
                        job.getImageId()
                );

                jobRepository.deleteById(jobId);
            });

            logger.info(
                    "Trabajo de eliminación completado: {}",
                    jobId
            );

        } catch (RuntimeException exception) {

            logger.warn(
                    "Error procesando eliminación {}. Se reintentará.",
                    jobId,
                    exception
            );

            try {
                scheduleRetry(jobId);
            } catch (RuntimeException retryException) {
                logger.error(
                        "No se pudo actualizar el reintento {}",
                        jobId,
                        retryException
                );
            }
        }
    }

    private void scheduleRetry(UUID jobId) {

        transactionTemplate.executeWithoutResult(status -> {

            ImageDeletionJob current = jobRepository
                    .findById(jobId)
                    .orElse(null);

            if (current == null) {
                return;
            }

            // Retroceso exponencial, limitado a 5 minutos.
            int exponent = Math.min(
                    current.getAttempts(),
                    6
            );

            long delaySeconds = Math.min(
                    300L,
                    5L * (1L << exponent)
            );

            current.scheduleRetry(
                    Instant.now().plusSeconds(delaySeconds),
                    "El servicio externo o la base de datos "
                            + "no completó la eliminación."
            );

            jobRepository.save(current);
        });
    }
}
