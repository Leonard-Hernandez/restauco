package com.lsoftware.restauco.infrastructure.floor.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record TableRequest(@Min(1) @Max(9999) int number, @Size(max = 40) String label, boolean active) {}
