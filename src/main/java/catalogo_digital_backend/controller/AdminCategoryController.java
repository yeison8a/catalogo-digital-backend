
package catalogo_digital_backend.controller;

import catalogo_digital_backend.dto.category.CategoryRequest;
import catalogo_digital_backend.dto.category.CategoryResponse;
import catalogo_digital_backend.dto.category.CategoryStatusRequest;
import catalogo_digital_backend.service.CategoryService;

import jakarta.validation.Valid;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@Profile("local")
@RequestMapping("/api/v1/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(
            CategoryService categoryService
    ) {
        this.categoryService = categoryService;
    }

    // Consultar todas las categorías
    @GetMapping
    public List<CategoryResponse> findAll() {
        return categoryService.findAll();
    }

    // Consultar una categoría por ID
    @GetMapping("/{id}")
    public CategoryResponse findById(
            @PathVariable UUID id
    ) {
        return categoryService.findById(id);
    }

    // Crear una categoría
    @PostMapping
    public ResponseEntity<CategoryResponse> create(
            @Valid @RequestBody CategoryRequest request
    ) {
        CategoryResponse created =
                categoryService.create(request);

        URI location = URI.create(
                "/api/v1/admin/categories/" + created.id()
        );

        return ResponseEntity
                .created(location)
                .body(created);
    }

    // Modificar el nombre
    @PutMapping("/{id}")
    public CategoryResponse updateName(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest request
    ) {
        return categoryService.updateName(id, request);
    }

    // Activar o desactivar
    @PatchMapping("/{id}/status")
    public ResponseEntity<CategoryResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryStatusRequest request
    ) {
        CategoryResponse updated =
                categoryService.updateStatus(
                        id,
                        request.active()
                );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(updated);
    }
}
