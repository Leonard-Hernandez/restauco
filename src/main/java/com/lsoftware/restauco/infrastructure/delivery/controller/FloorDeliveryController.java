package com.lsoftware.restauco.infrastructure.delivery.controller;

import com.lsoftware.restauco.infrastructure.delivery.dto.DeliveryResponse;
import com.lsoftware.restauco.usecase.delivery.GetDeliveryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Deliveries for the waiter. Confirming, rejecting and handing to the courier use the ticket
 * endpoints; charging uses the bill's (sessions/{id}/close), like a table.
 */
@RestController
@RequestMapping("/api/panel/floor/deliveries")
@RequiredArgsConstructor
@Tag(name = "Floor (waiter)", description = "Tables, orders and bills")
@SecurityRequirement(name = "bearerAuth")
public class FloorDeliveryController {

    private final GetDeliveryUseCase deliveries;

    @GetMapping
    @Operation(summary = "Deliveries not charged yet, oldest first")
    public List<DeliveryResponse> open() {
        return deliveries.open().stream().map(DeliveryResponse::from).toList();
    }
}
