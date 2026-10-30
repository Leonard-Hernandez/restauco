package com.lsoftware.restauco.infrastructure.member.controller;

import com.lsoftware.restauco.entity.member.model.TenantMember;
import com.lsoftware.restauco.infrastructure.config.security.PanelAuthorizationManager;
import com.lsoftware.restauco.infrastructure.member.dto.StaffResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/panel/me")
@Tag(name = "Staff (panel)", description = "Who is using the panel")
@SecurityRequirement(name = "bearerAuth")
public class PanelMeController {

    @GetMapping
    @Operation(summary = "Role of the signed-in user in this restaurant (403 if not staff)")
    public StaffResponse me(@RequestAttribute(PanelAuthorizationManager.MEMBER_ATTRIBUTE) TenantMember member) {
        return StaffResponse.from(member);
    }
}
