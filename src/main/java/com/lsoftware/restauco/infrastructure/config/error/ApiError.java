package com.lsoftware.restauco.infrastructure.config.error;

import java.time.OffsetDateTime;

public record ApiError(String code, String message, OffsetDateTime timestamp) {
    public ApiError(String code, String message) {
        this(code, message, OffsetDateTime.now());
    }
}
