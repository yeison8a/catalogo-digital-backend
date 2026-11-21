
package catalogo_digital_backend.repository;

import catalogo_digital_backend.entity.ImageDeletionJob;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ImageDeletionJobRepository
        extends JpaRepository<ImageDeletionJob, UUID> {

    boolean existsByImageId(UUID imageId);

    List<ImageDeletionJob>
    findTop20ByNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            Instant currentTime
    );
}
