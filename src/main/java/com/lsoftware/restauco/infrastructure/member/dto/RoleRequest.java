package com.lsoftware.restauco.infrastructure.member.dto;

import com.lsoftware.restauco.entity.member.model.MemberRole;
import jakarta.validation.constraints.NotNull;

public record RoleRequest(@NotNull MemberRole role) {}
