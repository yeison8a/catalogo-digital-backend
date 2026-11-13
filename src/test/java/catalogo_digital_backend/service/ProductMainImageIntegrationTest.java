
package catalogo_digital_backend.service;

import catalogo_digital_backend.dto.product.ProductResponse;
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
class ProductMainImageIntegrationTest {

    @Autowired
    private ProductService productService;

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
                "Producto con imágenes",
                new BigDecimal("95000.00"),
                category
        );

        return productRepository.saveAndFlush(product);
    }

    @Test
    void shouldReturnNullWhenProductHasNoImages() {

        Product product = createTestProduct();

        ProductResponse response =
                productService.findById(product.getId());

        assertNull(response.mainImageUrl());
    }

    @Test
    void shouldReturnMainImageUrl() {

        Product product = createTestProduct();

        String firstUrl =
                "https://example.com/front.jpg";

        ProductImageResponse firstImage =
                imageService.addImage(
                        product.getId(),
                        new ProductImageRequest(firstUrl)
                );

        imageService.addImage(
                product.getId(),
                new ProductImageRequest(
                        "https://example.com/side.jpg"
                )
        );

        ProductResponse response =
                productService.findById(product.getId());

        assertEquals(
                firstUrl,
                response.mainImageUrl()
        );

        assertTrue(firstImage.mainImage());
    }

    @Test
    void shouldUpdateMainImageUrlAfterChangingMain() {

        Product product = createTestProduct();

        imageService.addImage(
                product.getId(),
                new ProductImageRequest(
                        "https://example.com/front.jpg"
                )
        );

        String secondUrl =
                "https://example.com/side.jpg";

        ProductImageResponse secondImage =
                imageService.addImage(
                        product.getId(),
                        new ProductImageRequest(secondUrl)
                );

        imageService.setMainImage(
                product.getId(),
                secondImage.id()
        );

        ProductResponse response =
                productService.findById(product.getId());

        assertEquals(
                secondUrl,
                response.mainImageUrl()
        );
    }

    @Test
    void shouldIncludeMainImageInProductLists() {

        Product firstProduct = createTestProduct();
        Product secondProduct = createTestProduct();

        String imageUrl =
                "https://example.com/catalog-image.jpg";

        imageService.addImage(
                firstProduct.getId(),
                new ProductImageRequest(imageUrl)
        );

        List<ProductResponse> products =
                productService.findPublished();

        ProductResponse first = products.stream()
                .filter(p -> p.id().equals(firstProduct.getId()))
                .findFirst()
                .orElseThrow();

        ProductResponse second = products.stream()
                .filter(p -> p.id().equals(secondProduct.getId()))
                .findFirst()
                .orElseThrow();

        assertEquals(
                imageUrl,
                first.mainImageUrl()
        );

        assertNull(second.mainImageUrl());
    }

    @Test
    void shouldRemoveMainImageUrlWhenLastImageIsDeleted() {

        Product product = createTestProduct();

        ProductImageResponse image =
                imageService.addImage(
                        product.getId(),
                        new ProductImageRequest(
                                "https://example.com/only-image.jpg"
                        )
                );

        imageService.deleteImage(
                product.getId(),
                image.id()
        );

        ProductResponse response =
                productService.findById(product.getId());

        assertNull(response.mainImageUrl());
    }
}
