package com.lsoftware.restauco.infrastructure.menu.controller;

import com.lsoftware.restauco.entity.tenant.gateway.CurrentTenantGateway;
import com.lsoftware.restauco.infrastructure.menu.dto.AvailabilityRequest;
import com.lsoftware.restauco.infrastructure.menu.dto.DishResponse;
import com.lsoftware.restauco.infrastructure.menu.dto.IngredientResponse;
import com.lsoftware.restauco.usecase.menu.ManageDishUseCase;
import com.lsoftware.restauco.usecase.menu.ManageIngredientUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** What ran out today: whole dishes, or ingredients (which take every dish and option made with them). */
@RestController
@RequestMapping("/api/panel/kitchen")
@RequiredArgsConstructor
@Tag(name = "Kitchen", description = "Kitchen board")
@SecurityRequirement(name = "bearerAuth")
public class KitchenMenuController {

    private final ManageDishUseCase dishes;
    private final ManageIngredientUseCase ingredients;
    private final CurrentTenantGateway tenants;

    @PatchMapping("/dishes/{id}/availability")
    @Operation(summary = "Mark a dish sold out (or back in stock)")
    public DishResponse availability(@PathVariable Long id, @RequestBody AvailabilityRequest req) {
        return DishResponse.from(dishes.setAvailability(id, req.available()), tenants.current().slug());
    }

    @PatchMapping("/ingredients/{id}/availability")
    @Operation(summary = "Mark an ingredient sold out (or back): dishes that need it and options made with it follow")
    public IngredientResponse ingredientAvailability(@PathVariable Long id, @RequestBody AvailabilityRequest req) {
        return IngredientResponse.from(ingredients.setAvailability(id, req.available()));
    }
}
