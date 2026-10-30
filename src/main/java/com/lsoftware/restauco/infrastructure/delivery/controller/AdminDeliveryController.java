package com.lsoftware.restauco.infrastructure.delivery.controller;

import com.lsoftware.restauco.infrastructure.delivery.dto.DeliverySettingsRequest;
import com.lsoftware.restauco.usecase.delivery.ManageDeliverySettingsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/panel/admin/delivery")
@RequiredArgsConstructor
@Tag(name = "Delivery (owner)", description = "Delivery zone, fee and minimum order")
@SecurityRequirement(name = "bearerAuth")
public class AdminDeliveryController {

    private final ManageDeliverySettingsUseCase settings;

    @GetMapping
    public DeliverySettingsRequest get() {
        return DeliverySettingsRequest.from(settings.get());
    }

    @PutMapping
    @Operation(summary = "Turn delivery on or off and set where from, how far, the fee and the minimum order")
    public DeliverySettingsRequest update(@Valid @RequestBody DeliverySettingsRequest req) {
        return DeliverySettingsRequest.from(settings.update(req.toDomain()));
    }
}
