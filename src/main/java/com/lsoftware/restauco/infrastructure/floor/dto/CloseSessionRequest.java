package com.lsoftware.restauco.infrastructure.floor.dto;

import com.lsoftware.restauco.usecase.floor.CloseSessionUseCase.PaymentPart;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

/**
 * Either one method for the whole bill ({@code paymentMethodId}) or a split
 * ({@code payments}: method and amount each, adding up to the total).
 */
public record CloseSessionRequest(Long paymentMethodId, @Valid List<Part> payments) {

    public record Part(@NotNull Long paymentMethodId, @NotNull @PositiveOrZero Long amount) {}

    @AssertTrue(message = "send paymentMethodId or payments")
    public boolean isPaymentGiven() {
        return paymentMethodId != null || (payments != null && !payments.isEmpty());
    }

    public List<PaymentPart> parts() {
        if (payments == null || payments.isEmpty()) return List.of(new PaymentPart(paymentMethodId, null));
        return payments.stream().map(p -> new PaymentPart(p.paymentMethodId(), p.amount())).toList();
    }
}
