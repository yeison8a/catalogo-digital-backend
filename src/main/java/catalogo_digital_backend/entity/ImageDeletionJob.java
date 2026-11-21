
package catalogo_digital_backend.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "image_deletion_jobs",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_image_deletion_job_image",
        columnNames = "image_id"
    )
)
public class ImageDeletionJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "image_id", nullable = false)
    private UUID imageId;

    @Column(name = "cloudinary_public_id", length = 255)
    private String cloudinaryPublicId;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt = Instant.now();

    @Column(name = "last_error", length = 500)
    private String lastError;

    protected ImageDeletionJob() {
    }

    public ImageDeletionJob(
            UUID productId,
            UUID imageId,
            String cloudinaryPublicId
    ) {
        this.productId = productId;
        this.imageId = imageId;
        this.cloudinaryPublicId = cloudinaryPublicId;
        this.nextAttemptAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductId() {
        return productId;
    }

    public UUID getImageId() {
        return imageId;
    }

    public String getCloudinaryPublicId() {
        return cloudinaryPublicId;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void scheduleRetry(
            Instant nextAttemptAt,
            String error
    ) {
        this.attempts++;
        this.nextAttemptAt = nextAttemptAt;

        this.lastError = error == null
                ? "Error no especificado"
                : error.substring(
                        0,
                        Math.min(error.length(), 500)
                );
    }
}
