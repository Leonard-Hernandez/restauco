package com.lsoftware.restauco.infrastructure.delivery.controller;

import com.lsoftware.restauco.entity.delivery.exception.InvalidDeliveryException;
import com.lsoftware.restauco.entity.tenant.gateway.CurrentTenantGateway;
import com.lsoftware.restauco.infrastructure.delivery.PublicRateLimiter;
import com.lsoftware.restauco.infrastructure.delivery.dto.*;
import com.lsoftware.restauco.usecase.delivery.GetDeliveryUseCase;
import com.lsoftware.restauco.usecase.delivery.LocateDeliveryUseCase;
import com.lsoftware.restauco.usecase.delivery.PlaceDeliveryUseCase;
import com.lsoftware.restauco.usecase.floor.ManagePaymentMethodUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/** Ordering for delivery from the website, without an account. A waiter confirms every order. */
@RestController
@RequestMapping("/api/public/delivery")
@RequiredArgsConstructor
@Tag(name = "Delivery (public)", description = "Delivery orders from the website")
public class DeliveryController {

    private static final Duration WINDOW = Duration.ofMinutes(10);
    private static final int MAX_LOOKUPS = 40;
    private static final int MAX_ORDERS = 5;

    private final CurrentTenantGateway tenants;
    private final ManagePaymentMethodUseCase paymentMethods;
    private final LocateDeliveryUseCase locate;
    private final PlaceDeliveryUseCase place;
    private final GetDeliveryUseCase get;
    private final PublicRateLimiter limiter;

    @GetMapping
    @Operation(summary = "Whether the restaurant delivers now, where from, how far, fee and payment methods")
    public DeliveryInfoResponse info() {
        return DeliveryInfoResponse.from(tenants.current().delivery(), paymentMethods.listActive());
    }

    @PostMapping("/locate")
    @Operation(summary = "Find an address on the map (by text, or what is at a point) and whether it is in the zone")
    public List<QuoteResponse> locate(@Valid @RequestBody LocateRequest req, HttpServletRequest http) {
        limiter.check("locate", http.getRemoteAddr(), MAX_LOOKUPS, WINDOW);
        if (req.lat() != null && req.lng() != null) {
            return List.of(QuoteResponse.from(locate.at(req.lat(), req.lng())));
        }
        if (req.address() == null) throw new InvalidDeliveryException("ADDRESS_REQUIRED", "Write the street and number");
        return locate.search(req.address()).stream().map(QuoteResponse::from).toList();
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Order for delivery; a waiter confirms it before it reaches the kitchen")
    public DeliveryTrackResponse order(@Valid @RequestBody DeliveryOrderRequest req, HttpServletRequest http) {
        limiter.check("delivery", http.getRemoteAddr(), MAX_ORDERS, WINDOW);
        return DeliveryTrackResponse.from(place.execute(req.toCommand()));
    }

    @GetMapping("/orders/{token}")
    @Operation(summary = "Follow a delivery order")
    public DeliveryTrackResponse track(@PathVariable UUID token) {
        return DeliveryTrackResponse.from(get.byToken(token));
    }
}
