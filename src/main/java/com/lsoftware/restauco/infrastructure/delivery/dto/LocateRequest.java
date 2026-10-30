package com.lsoftware.restauco.infrastructure.delivery.dto;

import jakarta.validation.constraints.Size;

/** Either the address as written, or a point (GPS or a dragged pin). In the body: it is personal data. */
public record LocateRequest(@Size(max = 160) String address, Double lat, Double lng) {}
