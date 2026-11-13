
package catalogo_digital_backend.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CloudinaryImageStorageServiceTest {

    private Cloudinary cloudinary;
    private Uploader uploader;
    private CloudinaryImageStorageService storageService;

    @BeforeEach
    void setUp() {

        cloudinary = mock(Cloudinary.class);
        uploader = mock(Uploader.class);

        when(cloudinary.uploader()).thenReturn(uploader);

        storageService =
                new CloudinaryImageStorageService(cloudinary);
    }

    // Generar una imagen PNG real en memoria.
    private MockMultipartFile createValidImage()
            throws Exception {

        BufferedImage image = new BufferedImage(
                100,
                100,
                BufferedImage.TYPE_INT_RGB
        );

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        assertTrue(ImageIO.write(image, "png", output));

        return new MockMultipartFile(
                "file",
                "producto.png",
                "image/png",
                output.toByteArray()
        );
    }

    @Test
    void shouldUploadValidImage() throws Exception {

        when(uploader.upload(
                any(byte[].class),
                anyMap()
        )).thenReturn(Map.of(
                "public_id", "catalogo-digital/products/test",
                "secure_url",
                "https://res.cloudinary.com/demo/image/upload/test.png"
        ));

        MockMultipartFile file = createValidImage();

        CloudinaryImageStorageService.StoredImage result =
                storageService.store(file);

        assertEquals(
                "catalogo-digital/products/test",
                result.publicId()
        );

        assertTrue(result.secureUrl().startsWith("https://"));

        assertEquals("image/png", result.contentType());

        verify(uploader, times(1)).upload(
                any(byte[].class),
                anyMap()
        );
    }

    @Test
    void shouldRejectFakeImage() {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "falso.jpg",
                "image/jpeg",
                "Esto no es una imagen".getBytes()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.store(file)
        );

        verifyNoInteractions(uploader);
    }

    @Test
    void shouldRejectEmptyImage() {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "vacio.png",
                "image/png",
                new byte[0]
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.store(file)
        );

        verifyNoInteractions(uploader);
    }

    @Test
    void shouldRejectOversizedImage() {

        byte[] bytes = new byte[
                5 * 1024 * 1024 + 1
        ];

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "grande.png",
                "image/png",
                bytes
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.store(file)
        );

        verifyNoInteractions(uploader);
    }
}
