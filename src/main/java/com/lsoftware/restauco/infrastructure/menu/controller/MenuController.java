package com.lsoftware.restauco.infrastructure.menu.controller;

import com.lsoftware.restauco.entity.tenant.gateway.CurrentTenantGateway;
import com.lsoftware.restauco.infrastructure.menu.dto.MenuResponse;
import com.lsoftware.restauco.usecase.menu.GetMenuUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/menu")
@RequiredArgsConstructor
@Tag(name = "Menu (public)", description = "Published menu with photos, ingredients and options")
public class MenuController {

    private final GetMenuUseCase useCase;
    private final CurrentTenantGateway tenants;

    @GetMapping
    @Operation(summary = "Published menu (sold-out dishes come with available=false)")
    public MenuResponse menu() {
        return MenuResponse.from(useCase.published(), tenants.current().slug());
    }
}
