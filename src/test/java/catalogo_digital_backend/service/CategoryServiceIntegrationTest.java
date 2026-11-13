
package catalogo_digital_backend.service;

import catalogo_digital_backend.dto.category.CategoryRequest;
import catalogo_digital_backend.dto.category.CategoryResponse;
import catalogo_digital_backend.entity.Category;
import catalogo_digital_backend.entity.Product;
import catalogo_digital_backend.repository.CategoryRepository;
import catalogo_digital_backend.repository.ProductRepository;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CategoryServiceIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void shouldCreateAndFindCategory() {

        String name = "Prueba " + UUID.randomUUID();

        // Crear categoría usando el servicio
        CategoryResponse created = categoryService.create(
                new CategoryRequest(name)
        );

        assertNotNull(created.id());
        assertEquals(name, created.name());
        assertTrue(created.active());

        // Consultarla por identificador
        CategoryResponse found =
                categoryService.findById(created.id());

        assertEquals(created.id(), found.id());
        assertEquals(name, found.name());
    }

    @Test
    void shouldRejectDuplicateNames() {

        String suffix = UUID.randomUUID().toString();

        String original = "Tecnología " + suffix;
        String duplicate = "TECNOLOGIA " + suffix;

        categoryService.create(
                new CategoryRequest(original)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> categoryService.create(
                        new CategoryRequest(duplicate)
                )
        );
    }

    @Test
    void shouldUpdateNameAndDeactivateEmptyCategory() {

        String name = "Prueba " + UUID.randomUUID();

        CategoryResponse created = categoryService.create(
                new CategoryRequest(name)
        );

        // Cambiar nombre
        CategoryResponse renamed =
                categoryService.updateName(
                        created.id(),
                        new CategoryRequest("Nueva " + name)
                );

        assertEquals("Nueva " + name, renamed.name());

        // Desactivar categoría sin productos
        CategoryResponse inactive =
                categoryService.updateStatus(
                        created.id(),
                        false
                );

        assertFalse(inactive.active());

        // Reactivar
        CategoryResponse active =
                categoryService.updateStatus(
                        created.id(),
                        true
                );

        assertTrue(active.active());
    }

    @Test
    void shouldRejectDeactivationWithPublishedProducts() {

        String name = "Prueba " + UUID.randomUUID();

        CategoryResponse created = categoryService.create(
                new CategoryRequest(name)
        );

        Category category = categoryRepository
                .findById(created.id())
                .orElseThrow();

        // Crear un producto publicado
        Product product = new Product(
                "Producto de prueba",
                "Descripción de prueba",
                new BigDecimal("45000.00"),
                category
        );

        productRepository.saveAndFlush(product);

        // Debe impedir la desactivación
        assertThrows(
                IllegalStateException.class,
                () -> categoryService.updateStatus(
                        created.id(),
                        false
                )
        );

        // La categoría debe continuar activa
        CategoryResponse result =
                categoryService.findById(created.id());

        assertTrue(result.active());
    }
}
