
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
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ProductImageServiceIntegrationTest {

    @Autowired
    private ProductImageService imageService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    private Product createTestProduct() {

        Category category = new Category(
                "Categoria " + UUID.randomUUID()
        );

        category = categoryRepository.saveAndFlush(category);

        Product product = new Product(
                "Producto de prueba",
                "Producto para probar las imágenes",
                new BigDecimal("95000.00"),
                category
        );

        return productRepository.saveAndFlush(product);
    }

    private ProductImageResponse addTestImage(
            UUID productId,
            int number
    ) {
        return imageService.addImage(
                productId,
                new ProductImageRequest(
                        "https://example.com/image-" + number + ".jpg"
                )
        );
    }

    @Test
    void shouldMakeFirstImageMain() {

        Product product = createTestProduct();

        ProductImageResponse image =
                addTestImage(product.getId(), 1);

        assertNotNull(image.id());
        assertEquals(0, image.sortOrder());
        assertTrue(image.mainImage());
        assertEquals(product.getId(), image.productId());
    }

    @Test
    void shouldRejectMoreThanFiveImages() {

        Product product = createTestProduct();

        for (int i = 1; i <= 5; i++) {
            addTestImage(product.getId(), i);
        }

        assertEquals(
                5,
                imageService.findByProduct(product.getId()).size()
        );

        assertThrows(
                IllegalStateException.class,
                () -> addTestImage(product.getId(), 6)
        );
    }

    @Test
    void shouldChangeMainImage() {

        Product product = createTestProduct();

        ProductImageResponse first =
                addTestImage(product.getId(), 1);

        ProductImageResponse second =
                addTestImage(product.getId(), 2);

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
    void shouldReorderImages() {

        Product product = createTestProduct();

        ProductImageResponse first =
                addTestImage(product.getId(), 1);

        ProductImageResponse second =
                addTestImage(product.getId(), 2);

        ProductImageResponse third =
                addTestImage(product.getId(), 3);

        List<ProductImageResponse> reordered =
                imageService.reorderImages(
                        product.getId(),
                        List.of(
                                third.id(),
                                first.id(),
                                second.id()
                        )
                );

        assertEquals(third.id(), reordered.get(0).id());
        assertEquals(first.id(), reordered.get(1).id());
        assertEquals(second.id(), reordered.get(2).id());

        assertEquals(0, reordered.get(0).sortOrder());
        assertEquals(1, reordered.get(1).sortOrder());
        assertEquals(2, reordered.get(2).sortOrder());

        // Reordenar no cambia la imagen principal.
        assertTrue(reordered.get(1).mainImage());
    }

    @Test
    void shouldPromoteAnotherImageAfterDeletingMain() {

        Product product = createTestProduct();

        ProductImageResponse first =
                addTestImage(product.getId(), 1);

        ProductImageResponse second =
                addTestImage(product.getId(), 2);

        imageService.deleteImage(
                product.getId(),
                first.id()
        );

        List<ProductImageResponse> remaining =
                imageService.findByProduct(product.getId());

        assertEquals(1, remaining.size());
        assertEquals(second.id(), remaining.get(0).id());
        assertEquals(0, remaining.get(0).sortOrder());
        assertTrue(remaining.get(0).mainImage());
    }

    @Test
    void shouldRejectInvalidUrl() {

        Product product = createTestProduct();

        assertThrows(
                IllegalArgumentException.class,
                () -> imageService.addImage(
                        product.getId(),
                        new ProductImageRequest("not-a-valid-url")
                )
        );
    }

    @Test
    void shouldRejectImageFromAnotherProduct() {

        Product firstProduct = createTestProduct();
        Product secondProduct = createTestProduct();

        ProductImageResponse image =
                addTestImage(firstProduct.getId(), 1);

        assertThrows(
                NoSuchElementException.class,
                () -> imageService.setMainImage(
                        secondProduct.getId(),
                        image.id()
                )
        );
    }
}
