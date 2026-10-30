package com.lsoftware.restauco.infrastructure.order.dto;

import jakarta.validation.constraints.Size;

public record CancelRequest(@Size(max = 140) String reason) {}
