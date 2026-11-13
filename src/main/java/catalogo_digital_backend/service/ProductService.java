
package catalogo_digital_backend.service;

import catalogo_digital_backend.dto.product.ProductRequest;
import catalogo_digital_backend.dto.product.ProductResponse;

import catalogo_digital_backend.entity.Category;
import catalogo_digital_backend.entity.Product;
import catalogo_digital_backend.entity.ProductImage;

import catalogo_digital_backend.repository.CategoryRepository;
import catalogo_digital_backend.repository.ProductRepository;
import catalogo_digital_backend.repository.ProductImageRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private static final BigDecimal MAX_PRICE =
            new BigDecimal("9999999999.99");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository imageRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductImageRepository imageRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.imageRepository = imageRepository;
    }

    // 1. Consultar todos los productos: administración
    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        List<Product> products = productRepository.findAll();

        return toResponses(products);
    }

    // 2. Consultar productos publicados y categorías activas
    @Transactional(readOnly = true)
    public List<ProductResponse> findPublished() {
        List<Product> products = productRepository
                .findByActiveTrue()
                .stream()
                .filter(product ->
                        product.getCategory().isActive()
                )
                .toList();

        return toResponses(products);
    }

    // 3. Buscar producto para administración
    @Transactional(readOnly = true)
    public ProductResponse findById(UUID id) {
        return toResponse(getProductOrThrow(id));
    }

    // 4. Buscar un producto públicamente
    @Transactional(readOnly = true)
    public ProductResponse findPublishedById(UUID id) {
        Product product = getProductOrThrow(id);

        if (!product.isActive()
                || !product.getCategory().isActive()) {
            throw new NoSuchElementException(
                    "No existe un producto público con ID: " + id
            );
        }

        return toResponse(product);
    }

    // 5. Crear producto
    @Transactional
    public ProductResponse create(ProductRequest request) {
        String name = validateName(request);
        String description =
                validateDescription(request.description());
        BigDecimal price = validatePrice(request.price());

        Category category =
                getActiveCategory(request.categoryId());

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

    // 6. Actualizar datos del producto
    @Transactional
    public ProductResponse update(
            UUID id,
            ProductRequest request
    ) {
        Product product = getProductOrThrow(id);

        String name = validateName(request);
        String description =
                validateDescription(request.description());
        BigDecimal price = validatePrice(request.price());

        Category category =
                getCategoryOrThrow(request.categoryId());

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

    // 7. Cambiar disponibilidad
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

    // 8. Publicar u ocultar
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

    // 9. Convertir un listado a DTOs eficientemente
    private List<ProductResponse> toResponses(
            List<Product> products
    ) {
        if (products.isEmpty()) {
            return List.of();
        }

        // Recolectar los IDs de los productos
        List<UUID> productIds = products.stream()
                .map(Product::getId)
                .toList();

        // Una consulta para todas las imágenes principales
        List<ProductImage> mainImages = imageRepository
                .findByProduct_IdInAndMainImageTrue(productIds);

        // Relacionar cada producto con su URL principal
        Map<UUID, String> mainImageUrls = mainImages.stream()
                .collect(
                        Collectors.toMap(
                                image -> image.getProduct().getId(),
                                ProductImage::getImageUrl,
                                (first, second) -> {
                                    throw new IllegalStateException(
                                            "Un producto tiene más de " +
                                            "una imagen principal."
                                    );
                                }
                        )
                );

        // Convertir los productos usando el mapa
        return products.stream()
                .sorted(
                        Comparator.comparing(
                                Product::getName,
                                String.CASE_INSENSITIVE_ORDER
                        )
                )
                .map(product -> toResponse(
                        product,
                        mainImageUrls.get(product.getId())
                ))
                .toList();
    }

    // 10. Convertir un producto individual
    private ProductResponse toResponse(Product product) {

        String mainImageUrl = imageRepository
                .findByProduct_IdAndMainImageTrue(
                        product.getId()
                )
                .map(ProductImage::getImageUrl)
                .orElse(null);

        return toResponse(product, mainImageUrl);
    }

    // 11. Construir ProductResponse
    private ProductResponse toResponse(
            Product product,
            String mainImageUrl
    ) {
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
                mainImageUrl
        );
    }

    // 12. Buscar producto
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

    // 13. Buscar categoría
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

    // 14. Verificar categoría activa
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

    // 15. Validar nombre
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

    // 16. Validar descripción
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

    // 17. Validar precio
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
                    "El precio no puede tener más de " +
                    "2 decimales significativos."
            );
        }
    }
}
