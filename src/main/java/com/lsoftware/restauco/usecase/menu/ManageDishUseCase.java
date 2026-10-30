package com.lsoftware.restauco.usecase.menu;

import com.lsoftware.restauco.entity.menu.exception.CategoryNotFoundException;
import com.lsoftware.restauco.entity.menu.exception.DishNotFoundException;
import com.lsoftware.restauco.entity.menu.exception.IngredientNotFoundException;
import com.lsoftware.restauco.entity.menu.exception.TagNotFoundException;
import com.lsoftware.restauco.entity.menu.gateway.CategoryGateway;
import com.lsoftware.restauco.entity.menu.gateway.DishGateway;
import com.lsoftware.restauco.entity.menu.gateway.ImageStorageGateway;
import com.lsoftware.restauco.entity.menu.gateway.IngredientGateway;
import com.lsoftware.restauco.entity.menu.gateway.TagGateway;
import com.lsoftware.restauco.entity.menu.model.Dish;
import com.lsoftware.restauco.entity.menu.model.DishIngredient;
import com.lsoftware.restauco.entity.menu.model.Ingredient;
import com.lsoftware.restauco.entity.menu.model.Tag;
import com.lsoftware.restauco.entity.order.gateway.OrderEventGateway;
import com.lsoftware.restauco.entity.tenant.gateway.CurrentTenantGateway;
import com.lsoftware.restauco.usecase.menu.dto.DishCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ManageDishUseCase {

    private final DishGateway dishes;
    private final CategoryGateway categories;
    private final IngredientGateway ingredients;
    private final TagGateway tags;
    private final ImageStorageGateway images;
    private final CurrentTenantGateway tenants;
    private final OrderEventGateway events;

    @Transactional
    public Dish create(DishCommand cmd) {
        return dishes.save(build(null, null, cmd));
    }

    @Transactional
    public Dish update(Long id, DishCommand cmd) {
        Dish existing = dishes.findById(id).orElseThrow(DishNotFoundException::new);
        return dishes.save(build(id, existing.imagePath(), cmd));
    }

    @Transactional
    public Dish setAvailability(Long id, boolean available) {
        Dish existing = dishes.findById(id).orElseThrow(DishNotFoundException::new);
        Dish saved = dishes.save(existing.withAvailability(available));
        events.menuChanged();
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Dish existing = dishes.findById(id).orElseThrow(DishNotFoundException::new);
        dishes.deleteById(id);
        deleteStoredImage(existing.imagePath());
    }

    /** Replaces the photo; the previous file is deleted. */
    @Transactional
    public Dish uploadImage(Long id, byte[] content) {
        Dish existing = dishes.findById(id).orElseThrow(DishNotFoundException::new);
        String extension = ImageFormat.extensionOf(content);
        String file = images.store(tenants.current().slug(), content, extension);
        Dish saved = dishes.save(existing.withImage(file));
        deleteStoredImage(existing.imagePath());
        return saved;
    }

    @Transactional
    public Dish removeImage(Long id) {
        Dish existing = dishes.findById(id).orElseThrow(DishNotFoundException::new);
        Dish saved = dishes.save(existing.withImage(null));
        deleteStoredImage(existing.imagePath());
        return saved;
    }

    private Dish build(Long id, String imagePath, DishCommand cmd) {
        categories.findById(cmd.categoryId()).orElseThrow(CategoryNotFoundException::new);

        Map<Long, Ingredient> catalog = ingredients.findAll().stream()
                .collect(Collectors.toMap(Ingredient::id, Function.identity()));
        // One entry per ingredient (the last one wins)
        Map<Long, DishCommand.IngredientRef> refs = new LinkedHashMap<>();
        for (DishCommand.IngredientRef ref : cmd.ingredients() != null ? cmd.ingredients() : List.<DishCommand.IngredientRef>of()) {
            refs.put(ref.ingredientId(), ref);
        }
        List<DishIngredient> dishIngredients = refs.values().stream().map(ref -> {
            Ingredient ingredient = catalog.get(ref.ingredientId());
            if (ingredient == null) throw new IngredientNotFoundException();
            return new DishIngredient(ingredient.id(), ingredient.name(), ingredient.allergen(), ref.removable(),
                    ingredient.available());
        }).toList();

        Set<Long> knownTags = tags.findAll().stream().map(Tag::id).collect(Collectors.toSet());
        List<Long> tagIds = cmd.tagIds() != null ? cmd.tagIds().stream().distinct().toList() : List.of();
        if (!knownTags.containsAll(tagIds)) throw new TagNotFoundException();

        return new Dish(id, cmd.categoryId(), cmd.name().strip(), ManageCategoryUseCase.blankToNull(cmd.description()),
                cmd.price(), imagePath, cmd.available(), cmd.active(), cmd.position(),
                dishIngredients, tagIds, ModifierRules.normalize(cmd.modifiers(), catalog.keySet()));
    }

    /** Demo images (absolute paths/URLs) are not ours to delete. */
    private void deleteStoredImage(String path) {
        if (path != null && !path.contains("/")) {
            images.delete(tenants.current().slug(), path);
        }
    }
}
