package com.lsoftware.restauco.infrastructure.tenant.controller;

import com.lsoftware.restauco.infrastructure.tenant.dto.TenantResponse;
import com.lsoftware.restauco.usecase.tenant.GetCurrentTenantUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/tenant")
@RequiredArgsConstructor
@Tag(name = "Tenant (public)", description = "Restaurant profile and branding (X-Tenant header)")
public class TenantController {

    private final GetCurrentTenantUseCase useCase;

    @GetMapping
    @Operation(summary = "Current restaurant profile")
    public TenantResponse current() {
        return TenantResponse.from(useCase.execute());
    }
}
