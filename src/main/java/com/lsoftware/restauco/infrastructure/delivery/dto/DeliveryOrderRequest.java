package com.lsoftware.restauco.infrastructure.delivery.dto;

import com.lsoftware.restauco.infrastructure.order.dto.OrderRequest;
import com.lsoftware.restauco.usecase.delivery.dto.DeliveryCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DeliveryOrderRequest(
        @NotBlank @Size(max = 80) String customerName,
        @NotBlank @Size(max = 20) String phone,
        @NotBlank @Size(max = 160) String address,
        @Size(max = 120) String addressDetail,
        @NotNull Double lat,
        @NotNull Double lng,
        @NotNull Long paymentMethodId,
        @Min(0) Long cashFor,
        @NotEmpty @Size(max = 30) List<OrderRequest.@Valid Line> lines
) {
    public DeliveryCommand toCommand() {
        return new DeliveryCommand(customerName, phone, address, addressDetail, lat, lng, paymentMethodId, cashFor,
                new OrderRequest(lines).toLines());
    }
}
