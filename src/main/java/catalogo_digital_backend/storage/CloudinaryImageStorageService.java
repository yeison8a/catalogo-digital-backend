
package catalogo_digital_backend.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Service
@Profile("local")
public class CloudinaryImageStorageService {

    private static final long MAX_FILE_SIZE =
            5L * 1024 * 1024;

    private static final int MAX_DIMENSION = 5000;

    private final Cloudinary cloudinary;

    public CloudinaryImageStorageService(
            Cloudinary cloudinary
    ) {
        this.cloudinary = cloudinary;
    }

    // 1. Subir una imagen a Cloudinary
    public StoredImage store(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Debes seleccionar una imagen."
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "La imagen no puede superar 5 MiB."
            );
        }

        try {
            byte[] bytes = file.getBytes();

            if (bytes.length == 0
                    || bytes.length > MAX_FILE_SIZE) {
                throw new IllegalArgumentException(
                        "El tamaño de la imagen no es válido."
                );
            }

            // Validar que sea una imagen real admitida.
            ImageFormat format = validateImage(bytes);

            // Generar identificador independiente
            // del nombre original enviado por el cliente.
            String publicId =
                    UUID.randomUUID().toString();

            Map<String, Object> options = ObjectUtils.asMap(
                    "folder", "catalogo-digital/products",
                    "public_id", publicId,
                    "resource_type", "image",
                    "overwrite", false
            );

            // Subir a Cloudinary.
            Map<?, ?> result = cloudinary.uploader()
                    .upload(bytes, options);

            Object returnedPublicId = result.get("public_id");
            Object secureUrl = result.get("secure_url");

            if (!(returnedPublicId instanceof String)
                    || !(secureUrl instanceof String)) {
                throw new IllegalStateException(
                        "Cloudinary devolvió una respuesta incompleta."
                );
            }

            String imageUrl = (String) secureUrl;

            if (!imageUrl.startsWith("https://")) {
                throw new IllegalStateException(
                        "Cloudinary no devolvió una URL HTTPS."
                );
            }

            return new StoredImage(
                    (String) returnedPublicId,
                    imageUrl,
                    format.contentType(),
                    bytes.length
            );

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudo subir la imagen a Cloudinary.",
                    exception
            );
        }
    }

    // 2. Validar contenido y dimensiones
    private ImageFormat validateImage(byte[] bytes)
            throws IOException {

        ImageFormat expected = detectFormat(bytes);

        try (ImageInputStream stream =
                     ImageIO.createImageInputStream(
                             new ByteArrayInputStream(bytes)
                     )) {

            if (stream == null) {
                throw new IllegalArgumentException(
                        "El archivo no es una imagen válida."
                );
            }

            Iterator<ImageReader> readers =
                    ImageIO.getImageReaders(stream);

            if (!readers.hasNext()) {
                throw new IllegalArgumentException(
                        "No se pudo interpretar la imagen."
                );
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(stream);

                String detectedFormat =
                        reader.getFormatName();

                boolean matches =
                        expected.extension().equals("png")
                                ? "png".equalsIgnoreCase(detectedFormat)
                                : "jpeg".equalsIgnoreCase(detectedFormat)
                                  || "jpg".equalsIgnoreCase(detectedFormat);

                if (!matches) {
                    throw new IllegalArgumentException(
                            "El formato de la imagen no coincide."
                    );
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                if (width <= 0 || height <= 0
                        || width > MAX_DIMENSION
                        || height > MAX_DIMENSION) {
                    throw new IllegalArgumentException(
                            "Las dimensiones de la imagen no son válidas."
                    );
                }

                if (reader.read(0) == null) {
                    throw new IllegalArgumentException(
                            "No se pudo decodificar la imagen."
                    );
                }

            } finally {
                reader.dispose();
            }
        }

        return expected;
    }

    // 3. Detectar PNG o JPEG por la firma
    private ImageFormat detectFormat(byte[] bytes) {

        // PNG
        if (bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50
                && bytes[2] == 0x4E
                && bytes[3] == 0x47
                && bytes[4] == 0x0D
                && bytes[5] == 0x0A
                && bytes[6] == 0x1A
                && bytes[7] == 0x0A) {

            return new ImageFormat("png", "image/png");
        }

        // JPEG
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF) {

            return new ImageFormat("jpg", "image/jpeg");
        }

        throw new IllegalArgumentException(
                "Solo se permiten imágenes PNG y JPEG."
        );
    }

    private record ImageFormat(
            String extension,
            String contentType
    ) {
    }

    // Información devuelta después de la subida
    public record StoredImage(
            String publicId,
            String secureUrl,
            String contentType,
            long size
    ) {
    }


public void delete(String publicId) {

    if (publicId == null || publicId.isBlank()) {
        throw new IllegalArgumentException(
                "El identificador de Cloudinary es obligatorio."
        );
    }

    try {
        Map<?, ?> result = cloudinary.uploader().destroy(
                publicId,
                ObjectUtils.asMap(
                        "resource_type", "image",
                        "invalidate", true
                )
        );

        Object status = result.get("result");

        if (!"ok".equals(status)
                && !"not found".equals(status)) {
            throw new IllegalStateException(
                    "Cloudinary no confirmó la eliminación."
            );
        }

    } catch (IOException exception) {
        throw new IllegalStateException(
                "No se pudo eliminar el archivo de Cloudinary.",
                exception
        );
    }
}

}
