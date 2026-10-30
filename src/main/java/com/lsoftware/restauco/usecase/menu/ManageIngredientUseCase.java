package com.lsoftware.restauco.usecase.menu;

import com.lsoftware.restauco.entity.menu.exception.DuplicateNameException;
import com.lsoftware.restauco.entity.menu.exception.IngredientNotFoundException;
import com.lsoftware.restauco.entity.menu.gateway.IngredientGateway;
import com.lsoftware.restauco.entity.menu.model.Ingredient;
import com.lsoftware.restauco.entity.order.gateway.OrderEventGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ManageIngredientUseCase {

    private final IngredientGateway gateway;
    private final OrderEventGateway events;

    @Transactional
    public Ingredient create(String name, boolean allergen) {
        String clean = name.strip();
        if (gateway.existsByName(clean, null)) throw new DuplicateNameException();
        return gateway.save(new Ingredient(null, clean, allergen));
    }

    @Transactional
    public Ingredient update(Long id, String name, boolean allergen) {
        Ingredient existing = gateway.findById(id).orElseThrow(IngredientNotFoundException::new);
        String clean = name.strip();
        if (gateway.existsByName(clean, id)) throw new DuplicateNameException();
        return gateway.save(new Ingredient(id, clean, allergen, existing.available()));
    }

    /**
     * Ran out (or came back). Dishes that cannot go without it stop being orderable, and
     * options made with it cannot be chosen, until it is back.
     */
    @Transactional
    public Ingredient setAvailability(Long id, boolean available) {
        Ingredient existing = gateway.findById(id).orElseThrow(IngredientNotFoundException::new);
        Ingredient saved = gateway.save(existing.withAvailability(available));
        events.menuChanged();
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        gateway.findById(id).orElseThrow(IngredientNotFoundException::new);
        gateway.deleteById(id);
    }
}
