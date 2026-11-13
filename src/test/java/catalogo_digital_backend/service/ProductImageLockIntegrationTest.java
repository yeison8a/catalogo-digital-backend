
package catalogo_digital_backend.service;

import catalogo_digital_backend.dto.product.image.ProductImageRequest;
import catalogo_digital_backend.dto.product.image.ProductImageResponse;

import catalogo_digital_backend.entity.Category;
import catalogo_digital_backend.entity.Product;

import catalogo_digital_backend.repository.CategoryRepository;
import catalogo_digital_backend.repository.ProductRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ProductImageLockIntegrationTest {

    @Autowired
    private ProductImageService imageService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Product createProduct() {

        Category category = categoryRepository.saveAndFlush(
                new Category("Categoria " + UUID.randomUUID())
        );

        return productRepository.saveAndFlush(
                new Product(
                        "Producto de prueba",
                        "Pruebas de galería",
                        new BigDecimal("95000.00"),
                        category
                )
        );
    }

    private ProductImageResponse addImage(
            UUID productId,
            int number
    ) {
        return imageService.addImage(
                productId,
                new ProductImageRequest(
                        "https://example.com/test-" + number + ".jpg"
                )
        );
    }

    @Test
    void shouldRespectFiveImageLimit() {

        Product product = createProduct();

        for (int i = 1; i <= 5; i++) {
            addImage(product.getId(), i);
        }

        assertEquals(
                5,
                imageService.findByProduct(product.getId()).size()
        );

        assertThrows(
                IllegalStateException.class,
                () -> addImage(product.getId(), 6)
        );
    }

    @Test
    void shouldKeepOnlyOneMainImage() {

        Product product = createProduct();

        ProductImageResponse first =
                addImage(product.getId(), 1);

        ProductImageResponse second =
                addImage(product.getId(), 2);

        imageService.setMainImage(
                product.getId(),
                second.id()
        );

        List<ProductImageResponse> images =
                imageService.findByProduct(product.getId());

        assertEquals(
                1,
                images.stream()
                        .filter(ProductImageResponse::mainImage)
                        .count()
        );

        assertFalse(
                images.stream()
                        .filter(i -> i.id().equals(first.id()))
                        .findFirst()
                        .orElseThrow()
                        .mainImage()
        );

        assertTrue(
                images.stream()
                        .filter(i -> i.id().equals(second.id()))
                        .findFirst()
                        .orElseThrow()
                        .mainImage()
        );
    }

    @Test
    void shouldPromoteRemainingImageAfterDelete() {

        Product product = createProduct();

        ProductImageResponse first =
                addImage(product.getId(), 1);

        ProductImageResponse second =
                addImage(product.getId(), 2);

        imageService.deleteImage(
                product.getId(),
                first.id()
        );

        List<ProductImageResponse> images =
                imageService.findByProduct(product.getId());

        assertEquals(1, images.size());
        assertEquals(second.id(), images.get(0).id());
        assertTrue(images.get(0).mainImage());
    }
}
