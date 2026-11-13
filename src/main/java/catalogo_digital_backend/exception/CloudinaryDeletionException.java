
package catalogo_digital_backend.exception;

public class CloudinaryDeletionException
        extends RuntimeException {

    public CloudinaryDeletionException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
