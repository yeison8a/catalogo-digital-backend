
package catalogo_digital_backend.service;

import catalogo_digital_backend.dto.category.CategoryRequest;
import catalogo_digital_backend.dto.category.CategoryResponse;
import catalogo_digital_backend.entity.Category;
import catalogo_digital_backend.repository.CategoryRepository;
import catalogo_digital_backend.repository.ProductRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    // Inyección de dependencias por constructor
    public CategoryService(
            CategoryRepository categoryRepository,
            ProductRepository productRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    // Consultar todas las categorías
    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll()
                .stream()
                .sorted(
                        Comparator.comparing(
                                Category::getName,
                                String.CASE_INSENSITIVE_ORDER
                        )
                )
                .map(this::toResponse)
                .toList();
    }

    // Consultar solo las categorías activas
    @Transactional(readOnly = true)
    public List<CategoryResponse> findActive() {
        return categoryRepository
                .findByActiveTrueOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Buscar una categoría por su UUID
    @Transactional(readOnly = true)
    public CategoryResponse findById(UUID id) {
        Category category = getCategoryOrThrow(id);

        return toResponse(category);
    }

    // Crear una categoría
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = validateName(request);

        validateUniqueName(name, null);

        Category category = new Category(name);

        Category savedCategory =
                categoryRepository.saveAndFlush(category);

        return toResponse(savedCategory);
    }

    // Modificar el nombre de una categoría
    @Transactional
    public CategoryResponse updateName(
            UUID id,
            CategoryRequest request
    ) {
        Category category = getCategoryOrThrow(id);

        String name = validateName(request);

        validateUniqueName(name, id);

        category.setName(name);

        Category updatedCategory =
                categoryRepository.saveAndFlush(category);

        return toResponse(updatedCategory);
    }

    // Activar o desactivar una categoría
    @Transactional
    public CategoryResponse updateStatus(
            UUID id,
            boolean active
    ) {
        Category category = getCategoryOrThrow(id);

        // Si ya tiene el estado solicitado, no modificar
        if (category.isActive() == active) {
            return toResponse(category);
        }

        // No desactivar categorías con productos publicados
        if (!active) {
            long publishedProducts =
                    productRepository
                            .countByCategory_IdAndActiveTrue(id);

            if (publishedProducts > 0) {
                throw new IllegalStateException(
                        "No se puede desactivar la categoría " +
                        "porque tiene productos publicados."
                );
            }
        }

        category.setActive(active);

        Category updatedCategory =
                categoryRepository.saveAndFlush(category);

        return toResponse(updatedCategory);
    }

    // Buscar la entidad o informar que no existe
    private Category getCategoryOrThrow(UUID id) {
        Objects.requireNonNull(
                id,
                "El identificador es obligatorio."
        );

        return categoryRepository.findById(id)
                .orElseThrow(
                        () -> new NoSuchElementException(
                                "No existe una categoría con ID: " + id
                        )
                );
    }

    // Validar y limpiar el nombre recibido
    private String validateName(CategoryRequest request) {
        if (request == null || request.name() == null) {
            throw new IllegalArgumentException(
                    "El nombre de la categoría es obligatorio."
            );
        }

        String name = request.name().trim();

        if (name.length() < 2 || name.length() > 100) {
            throw new IllegalArgumentException(
                    "El nombre debe tener entre 2 y 100 caracteres."
            );
        }

        return name;
    }

    // Evitar nombres duplicados
    private void validateUniqueName(
            String name,
            UUID excludeId
    ) {
        String normalizedName = normalizeName(name);

        boolean exists = categoryRepository.findAll()
                .stream()
                .anyMatch(category -> {
                    boolean isDifferentCategory =
                            !category.getId().equals(excludeId);

                    boolean sameName =
                            normalizeName(category.getName())
                                    .equals(normalizedName);

                    return isDifferentCategory && sameName;
                });

        if (exists) {
            throw new IllegalArgumentException(
                    "Ya existe una categoría con ese nombre."
            );
        }
    }

    // Normalizar mayúsculas y tildes
    private String normalizeName(String value) {
        String normalized = Normalizer.normalize(
                value.trim(),
                Normalizer.Form.NFD
        );

        return normalized
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
    }

    // Convertir Entity a DTO
    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.isActive()
        );
    }
}
