package com.lsoftware.restauco.infrastructure.floor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PaymentMethodRequest(@NotBlank @Size(max = 40) String name, int position, boolean active) {}
