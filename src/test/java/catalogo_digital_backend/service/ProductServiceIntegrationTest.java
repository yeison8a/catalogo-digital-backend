
package catalogo_digital_backend.service;

import catalogo_digital_backend.dto.product.ProductRequest;
import catalogo_digital_backend.dto.product.ProductResponse;
import catalogo_digital_backend.entity.Category;
import catalogo_digital_backend.repository.CategoryRepository;

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
class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryRepository categoryRepository;

    // Crear una categoría de prueba
    private Category createTestCategory() {
        Category category = new Category(
                "Test " + UUID.randomUUID()
        );

        return categoryRepository.saveAndFlush(category);
    }

    // Construir solicitud válida
    private ProductRequest buildRequest(UUID categoryId) {
        return new ProductRequest(
                "Audífonos de prueba",
                "Producto para comprobar ProductService",
                new BigDecimal("95000.00"),
                categoryId
        );
    }

    @Test
    void shouldCreateAndFindProduct() {
        Category category = createTestCategory();

        ProductResponse created = productService.create(
                buildRequest(category.getId())
        );

        assertNotNull(created.id());

        assertEquals(
                "Audífonos de prueba",
                created.name()
        );

        assertEquals(
                0,
                new BigDecimal("95000.00")
                        .compareTo(created.price())
        );

        assertTrue(created.active());
        assertTrue(created.available());

        assertEquals(
                category.getId(),
                created.categoryId()
        );

        ProductResponse found =
                productService.findById(created.id());

        assertEquals(created.id(), found.id());

        // El producto debe aparecer en el catálogo público
        List<ProductResponse> published =
                productService.findPublished();

        assertTrue(
                published.stream()
                        .anyMatch(p ->
                                p.id().equals(created.id())
                        )
        );
    }

    @Test
    void shouldRejectInvalidPrice() {
        Category category = createTestCategory();

        ProductRequest request = new ProductRequest(
                "Producto inválido",
                "Precio negativo",
                new BigDecimal("-1500.00"),
                category.getId()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.create(request)
        );
    }

    @Test
    void shouldRejectInactiveCategory() {
        Category category = createTestCategory();

        category.setActive(false);
        categoryRepository.saveAndFlush(category);

        assertThrows(
                IllegalStateException.class,
                () -> productService.create(
                        buildRequest(category.getId())
                )
        );
    }

    @Test
    void shouldUpdateAndHideProduct() {
        Category category = createTestCategory();

        ProductResponse created = productService.create(
                buildRequest(category.getId())
        );

        // Cambiar nombre, descripción y precio
        ProductRequest updatedRequest = new ProductRequest(
                "Audífonos actualizados",
                "Nueva descripción",
                new BigDecimal("120000.00"),
                category.getId()
        );

        ProductResponse updated = productService.update(
                created.id(),
                updatedRequest
        );

        assertEquals(
                "Audífonos actualizados",
                updated.name()
        );

        // Marcar como agotado
        ProductResponse unavailable =
                productService.updateAvailability(
                        created.id(),
                        false
                );

        assertFalse(unavailable.available());
        assertTrue(unavailable.active());

        // Ocultar el producto
        ProductResponse hidden =
                productService.updatePublication(
                        created.id(),
                        false
                );

        assertFalse(hidden.active());

        // Ya no debe aparecer públicamente
        assertFalse(
                productService.findPublished()
                        .stream()
                        .anyMatch(p ->
                                p.id().equals(created.id())
                        )
        );

        // Pero sigue existiendo para administración
        assertNotNull(
                productService.findById(created.id())
        );
    }

    @Test
    void shouldRejectUnknownProduct() {
        UUID unknownId = UUID.randomUUID();

        assertThrows(
                NoSuchElementException.class,
                () -> productService.findById(unknownId)
        );
    }
}
