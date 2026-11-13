
package catalogo_digital_backend.service;

import catalogo_digital_backend.dto.product.ProductRequest;
import catalogo_digital_backend.dto.product.ProductResponse;
import catalogo_digital_backend.entity.Category;
import catalogo_digital_backend.entity.Product;
import catalogo_digital_backend.repository.CategoryRepository;
import catalogo_digital_backend.repository.ProductRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class ProductService {

    private static final BigDecimal MAX_PRICE =
            new BigDecimal("9999999999.99");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    // 1. Consultar todos los productos (administración)
    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAll()
                .stream()
                .sorted(
                        Comparator.comparing(
                                Product::getName,
                                String.CASE_INSENSITIVE_ORDER
                        )
                )
                .map(this::toResponse)
                .toList();
    }

    // 2. Consultar productos visibles públicamente
    @Transactional(readOnly = true)
    public List<ProductResponse> findPublished() {
        return productRepository.findByActiveTrue()
                .stream()
                .filter(product ->
                        product.getCategory().isActive()
                )
                .sorted(
                        Comparator.comparing(
                                Product::getName,
                                String.CASE_INSENSITIVE_ORDER
                        )
                )
                .map(this::toResponse)
                .toList();
    }

    // 3. Buscar un producto por su ID (administración)
    @Transactional(readOnly = true)
    public ProductResponse findById(UUID id) {
        return toResponse(getProductOrThrow(id));
    }

    // 4. Crear un producto
    @Transactional
    public ProductResponse create(ProductRequest request) {
        String name = validateName(request);
        String description = validateDescription(
                request.description()
        );
        BigDecimal price = validatePrice(request.price());

        Category category = getActiveCategory(
                request.categoryId()
        );

        Product product = new Product(
                name,
                description,
                price,
                category
        );

        Product saved =
                productRepository.saveAndFlush(product);

        return toResponse(saved);
    }

    // 5. Editar los datos principales
    @Transactional
    public ProductResponse update(
            UUID id,
            ProductRequest request
    ) {
        Product product = getProductOrThrow(id);

        String name = validateName(request);
        String description = validateDescription(
                request.description()
        );
        BigDecimal price = validatePrice(request.price());

        Category category = getCategoryOrThrow(
                request.categoryId()
        );

        // Un producto publicado debe pertenecer
        // a una categoría activa.
        if (product.isActive() && !category.isActive()) {
            throw new IllegalStateException(
                    "No se puede asignar una categoría inactiva " +
                    "a un producto publicado."
            );
        }

        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setCategory(category);

        Product saved =
                productRepository.saveAndFlush(product);

        return toResponse(saved);
    }

    // 6. Cambiar disponibilidad (agotado/disponible)
    @Transactional
    public ProductResponse updateAvailability(
            UUID id,
            boolean available
    ) {
        Product product = getProductOrThrow(id);

        product.setAvailable(available);

        return toResponse(
                productRepository.saveAndFlush(product)
        );
    }

    // 7. Publicar u ocultar un producto
    @Transactional
    public ProductResponse updatePublication(
            UUID id,
            boolean active
    ) {
        Product product = getProductOrThrow(id);

        if (active && !product.getCategory().isActive()) {
            throw new IllegalStateException(
                    "No se puede publicar un producto " +
                    "cuya categoría está inactiva."
            );
        }

        product.setActive(active);

        return toResponse(
                productRepository.saveAndFlush(product)
        );
    }


@Transactional(readOnly = true)
public ProductResponse findPublishedById(UUID id) {

    // Reutilizar la consulta existente
    Product product = getProductOrThrow(id);

    // Comprobar que el producto y su categoría
    // estén publicados/activos
    if (!product.isActive()
            || !product.getCategory().isActive()) {

        throw new NoSuchElementException(
                "No existe un producto público con ID: " + id
        );
    }

    return toResponse(product);
}

    // 8. Buscar producto existente
    private Product getProductOrThrow(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException(
                    "El identificador del producto es obligatorio."
            );
        }

        return productRepository.findById(id)
                .orElseThrow(
                        () -> new NoSuchElementException(
                                "No existe un producto con ID: " + id
                        )
                );
    }

    // 9. Buscar categoría existente
    private Category getCategoryOrThrow(UUID categoryId) {
        if (categoryId == null) {
            throw new IllegalArgumentException(
                    "La categoría es obligatoria."
            );
        }

        return categoryRepository.findById(categoryId)
                .orElseThrow(
                        () -> new NoSuchElementException(
                                "No existe una categoría con ID: " +
                                categoryId
                        )
                );
    }

    // 10. Obtener categoría activa
    private Category getActiveCategory(UUID categoryId) {
        Category category = getCategoryOrThrow(categoryId);

        if (!category.isActive()) {
            throw new IllegalStateException(
                    "No se puede crear un producto " +
                    "en una categoría inactiva."
            );
        }

        return category;
    }

    // 11. Validar el nombre
    private String validateName(ProductRequest request) {
        if (request == null || request.name() == null) {
            throw new IllegalArgumentException(
                    "El nombre del producto es obligatorio."
            );
        }

        String name = request.name().trim();

        if (name.length() < 2 || name.length() > 150) {
            throw new IllegalArgumentException(
                    "El nombre debe tener entre 2 y 150 caracteres."
            );
        }

        return name;
    }

    // 12. Validar descripción
    private String validateDescription(String description) {
        if (description == null) {
            return null;
        }

        String cleaned = description.trim();

        if (cleaned.length() > 2000) {
            throw new IllegalArgumentException(
                    "La descripción no puede superar 2000 caracteres."
            );
        }

        return cleaned.isEmpty() ? null : cleaned;
    }

    // 13. Validar y normalizar precio
    private BigDecimal validatePrice(BigDecimal price) {
        if (price == null) {
            throw new IllegalArgumentException(
                    "El precio es obligatorio."
            );
        }

        if (price.signum() < 0) {
            throw new IllegalArgumentException(
                    "El precio no puede ser negativo."
            );
        }

        if (price.compareTo(MAX_PRICE) > 0) {
            throw new IllegalArgumentException(
                    "El precio supera el máximo permitido."
            );
        }

        try {
            return price.setScale(
                    2,
                    RoundingMode.UNNECESSARY
            );
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "El precio no puede tener más de 2 decimales significativos."
            );
        }
    }

    // 14. Convertir entidad a DTO
    private ProductResponse toResponse(Product product) {
        Category category = product.getCategory();

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.isAvailable(),
                product.isActive(),
                category.getId(),
                category.getName(),
                null // La imagen principal se implementará después
        );
    }
}
