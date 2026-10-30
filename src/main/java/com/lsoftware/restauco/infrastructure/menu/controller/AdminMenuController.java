package com.lsoftware.restauco.infrastructure.menu.controller;

import com.lsoftware.restauco.entity.tenant.gateway.CurrentTenantGateway;
import com.lsoftware.restauco.infrastructure.menu.dto.*;
import com.lsoftware.restauco.usecase.menu.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/panel/admin")
@RequiredArgsConstructor
@Tag(name = "Menu (owner)", description = "Menu editor: categories, dishes, photos, ingredients, tags")
@SecurityRequirement(name = "bearerAuth")
public class AdminMenuController {

    private final GetMenuUseCase menu;
    private final ManageCategoryUseCase categories;
    private final ManageDishUseCase dishes;
    private final ManageIngredientUseCase ingredients;
    private final ManageTagUseCase tags;
    private final CurrentTenantGateway tenants;

    @GetMapping("/menu")
    @Operation(summary = "Whole menu, hidden items included")
    public MenuResponse menu() {
        return MenuResponse.from(menu.full(), slug());
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest req) {
        return CategoryResponse.from(categories.create(req.toDomain()));
    }

    @PutMapping("/categories/{id}")
    public CategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
        return CategoryResponse.from(categories.update(id, req.toDomain()));
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an empty category")
    public void deleteCategory(@PathVariable Long id) {
        categories.delete(id);
    }

    @PostMapping("/dishes")
    @ResponseStatus(HttpStatus.CREATED)
    public DishResponse createDish(@Valid @RequestBody DishRequest req) {
        return DishResponse.from(dishes.create(req.toCommand()), slug());
    }

    @PutMapping("/dishes/{id}")
    public DishResponse updateDish(@PathVariable Long id, @Valid @RequestBody DishRequest req) {
        return DishResponse.from(dishes.update(id, req.toCommand()), slug());
    }

    @DeleteMapping("/dishes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDish(@PathVariable Long id) {
        dishes.delete(id);
    }

    @PostMapping(path = "/dishes/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload or replace the dish photo (JPG, PNG or WebP, up to 5 MB)")
    public DishResponse uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        return DishResponse.from(dishes.uploadImage(id, file.getBytes()), slug());
    }

    @DeleteMapping("/dishes/{id}/image")
    public DishResponse removeImage(@PathVariable Long id) {
        return DishResponse.from(dishes.removeImage(id), slug());
    }

    @PostMapping("/ingredients")
    @ResponseStatus(HttpStatus.CREATED)
    public IngredientResponse createIngredient(@Valid @RequestBody IngredientRequest req) {
        return IngredientResponse.from(ingredients.create(req.name(), req.allergen()));
    }

    @PutMapping("/ingredients/{id}")
    public IngredientResponse updateIngredient(@PathVariable Long id, @Valid @RequestBody IngredientRequest req) {
        return IngredientResponse.from(ingredients.update(id, req.name(), req.allergen()));
    }

    @DeleteMapping("/ingredients/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an ingredient (also removes it from every dish)")
    public void deleteIngredient(@PathVariable Long id) {
        ingredients.delete(id);
    }

    @PostMapping("/tags")
    @ResponseStatus(HttpStatus.CREATED)
    public TagResponse createTag(@Valid @RequestBody TagRequest req) {
        return TagResponse.from(tags.create(req.name()));
    }

    @PutMapping("/tags/{id}")
    public TagResponse updateTag(@PathVariable Long id, @Valid @RequestBody TagRequest req) {
        return TagResponse.from(tags.update(id, req.name()));
    }

    @DeleteMapping("/tags/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTag(@PathVariable Long id) {
        tags.delete(id);
    }

    private String slug() {
        return tenants.current().slug();
    }
}
