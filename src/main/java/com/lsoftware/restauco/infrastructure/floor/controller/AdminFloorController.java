package com.lsoftware.restauco.infrastructure.floor.controller;

import com.lsoftware.restauco.infrastructure.floor.dto.*;
import com.lsoftware.restauco.usecase.floor.ManagePaymentMethodUseCase;
import com.lsoftware.restauco.usecase.floor.ManageTableUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/panel/admin")
@RequiredArgsConstructor
@Tag(name = "Floor (owner)", description = "Tables, QR codes and payment methods")
@SecurityRequirement(name = "bearerAuth")
public class AdminFloorController {

    private final ManageTableUseCase tables;
    private final ManagePaymentMethodUseCase paymentMethods;

    @GetMapping("/tables")
    public List<TableResponse> tables() {
        return tables.list().stream().map(TableResponse::from).toList();
    }

    @PostMapping("/tables")
    @ResponseStatus(HttpStatus.CREATED)
    public TableResponse createTable(@Valid @RequestBody TableRequest req) {
        return TableResponse.from(tables.create(req.number(), req.label(), req.active()));
    }

    @PutMapping("/tables/{id}")
    public TableResponse updateTable(@PathVariable Long id, @Valid @RequestBody TableRequest req) {
        return TableResponse.from(tables.update(id, req.number(), req.label(), req.active()));
    }

    @PostMapping("/tables/{id}/qr")
    @Operation(summary = "New QR for the table (old prints stop working)")
    public TableResponse regenerateQr(@PathVariable Long id) {
        return TableResponse.from(tables.regenerateQr(id));
    }

    @GetMapping("/payment-methods")
    public List<PaymentMethodResponse> paymentMethods() {
        return paymentMethods.list().stream().map(PaymentMethodResponse::from).toList();
    }

    @PostMapping("/payment-methods")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentMethodResponse createPaymentMethod(@Valid @RequestBody PaymentMethodRequest req) {
        return PaymentMethodResponse.from(paymentMethods.create(req.name(), req.position(), req.active()));
    }

    @PutMapping("/payment-methods/{id}")
    public PaymentMethodResponse updatePaymentMethod(@PathVariable Long id, @Valid @RequestBody PaymentMethodRequest req) {
        return PaymentMethodResponse.from(paymentMethods.update(id, req.name(), req.position(), req.active()));
    }
}
