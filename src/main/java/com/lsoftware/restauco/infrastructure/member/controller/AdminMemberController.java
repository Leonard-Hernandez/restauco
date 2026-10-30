package com.lsoftware.restauco.infrastructure.member.controller;

import com.lsoftware.restauco.infrastructure.member.dto.InviteRequest;
import com.lsoftware.restauco.infrastructure.member.dto.MemberResponse;
import com.lsoftware.restauco.infrastructure.member.dto.RoleRequest;
import com.lsoftware.restauco.usecase.member.ManageMemberUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/panel/admin/members")
@RequiredArgsConstructor
@Tag(name = "Team (owner)", description = "Who can use the panel and in which role")
@SecurityRequirement(name = "bearerAuth")
public class AdminMemberController {

    private final ManageMemberUseCase members;

    @GetMapping
    public List<MemberResponse> list() {
        return members.list().stream().map(MemberResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Invite a Google email; it links on that person's first login")
    public MemberResponse invite(@Valid @RequestBody InviteRequest req) {
        return MemberResponse.from(members.invite(req.email(), req.role()));
    }

    @PutMapping("/{id}")
    public MemberResponse changeRole(@PathVariable Long id, @Valid @RequestBody RoleRequest req,
                                     @AuthenticationPrincipal Jwt jwt) {
        return MemberResponse.from(members.changeRole(id, req.role(), jwt.getSubject()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        members.remove(id, jwt.getSubject());
    }
}
