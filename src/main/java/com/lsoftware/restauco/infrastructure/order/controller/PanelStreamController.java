package com.lsoftware.restauco.infrastructure.order.controller;

import com.lsoftware.restauco.entity.tenant.gateway.CurrentTenantGateway;
import com.lsoftware.restauco.infrastructure.realtime.EventLog;
import com.lsoftware.restauco.infrastructure.realtime.SseHub;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/panel")
@RequiredArgsConstructor
@Tag(name = "Staff (panel)", description = "Live updates")
@SecurityRequirement(name = "bearerAuth")
public class PanelStreamController {

    private final SseHub hub;
    private final CurrentTenantGateway tenants;

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Every ticket and floor change of the restaurant (Server-Sent Events)")
    public SseEmitter stream() {
        return hub.staff(tenants.current().id());
    }

    @GetMapping("/events")
    @Operation(summary = "The same events by polling, for proxies that buffer streams")
    public EventLog.Batch events(@RequestParam(required = false) Long after) {
        return hub.staffSince(tenants.current().id(), after);
    }
}
