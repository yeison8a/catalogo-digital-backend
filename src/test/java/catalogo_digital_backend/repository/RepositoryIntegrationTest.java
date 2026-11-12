
package catalogo_digital_backend.repository;

import catalogo_digital_backend.entity.Category;
import catalogo_digital_backend.entity.Product;
import catalogo_digital_backend.entity.ProductImage;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RepositoryIntegrationTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Test
    void shouldSaveAndFindRelatedEntities() {

        // 1. Crear una categoría de prueba
        String categoryName =
                "Prueba " + UUID.randomUUID();

        Category category = new Category(categoryName);

        category = categoryRepository.saveAndFlush(
                category
        );

        assertNotNull(category.getId());

        // 2. Crear un producto relacionado
        Product product = new Product(
                "Producto de prueba",
                "Producto para verificar repositorios",
                new BigDecimal("90000.00"),
                category
        );

        product = productRepository.saveAndFlush(
                product
        );

        assertNotNull(product.getId());

        // 3. Crear una fotografía
        ProductImage image = new ProductImage(
                "https://example.com/test-image.jpg",
                0,
                true,
                product
        );

        image = productImageRepository.saveAndFlush(
                image
        );

        assertNotNull(image.getId());

        // 4. Consultar la categoría
        boolean categoryExists =
                categoryRepository.existsByNameIgnoreCase(
                        categoryName.toUpperCase()
                );

        assertTrue(categoryExists);

        // 5. Consultar productos de la categoría
        List<Product> products =
                productRepository.findByCategory_Id(
                        category.getId()
                );

        assertEquals(1, products.size());

        assertEquals(
                "Producto de prueba",
                products.get(0).getName()
        );

        // 6. Consultar productos publicados
        long publishedProducts =
                productRepository
                        .countByCategory_IdAndActiveTrue(
                                category.getId()
                        );

        assertEquals(1L, publishedProducts);

        // 7. Consultar las fotografías del producto
        List<ProductImage> images =
                productImageRepository
                        .findByProduct_IdOrderBySortOrderAsc(
                                product.getId()
                        );

        assertEquals(1, images.size());

        assertEquals(
                0,
                images.get(0).getSortOrder()
        );

        // 8. Consultar la fotografía principal
        Optional<ProductImage> mainImage =
                productImageRepository
                        .findByProduct_IdAndMainImageTrue(
                                product.getId()
                        );

        assertTrue(mainImage.isPresent());

        assertEquals(
                image.getId(),
                mainImage.orElseThrow().getId()
        );

        // Los registros de esta prueba se revierten
        // automáticamente al finalizar la transacción.
    }
}
