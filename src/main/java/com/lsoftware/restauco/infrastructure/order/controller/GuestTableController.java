package com.lsoftware.restauco.infrastructure.order.controller;

import com.lsoftware.restauco.entity.floor.exception.TableNotFoundException;
import com.lsoftware.restauco.entity.floor.gateway.TableGateway;
import com.lsoftware.restauco.entity.tenant.gateway.CurrentTenantGateway;
import com.lsoftware.restauco.infrastructure.order.dto.GuestTableResponse;
import com.lsoftware.restauco.infrastructure.order.dto.OrderRequest;
import com.lsoftware.restauco.infrastructure.order.dto.TicketResponse;
import com.lsoftware.restauco.infrastructure.realtime.EventLog;
import com.lsoftware.restauco.infrastructure.realtime.SseHub;
import com.lsoftware.restauco.usecase.order.GetGuestTableUseCase;
import com.lsoftware.restauco.usecase.order.PlaceOrderUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/** The table QR: guests see their rounds and order without an account (a waiter approves). */
@RestController
@RequestMapping("/api/public/tables/{token}")
@RequiredArgsConstructor
@Tag(name = "Table (QR)", description = "Guest ordering from the table")
public class GuestTableController {

    private final GetGuestTableUseCase getTable;
    private final PlaceOrderUseCase placeOrder;
    private final TableGateway tables;
    private final CurrentTenantGateway tenants;
    private final SseHub hub;

    @GetMapping
    @Operation(summary = "The table and its current rounds")
    public GuestTableResponse table(@PathVariable UUID token) {
        return GuestTableResponse.from(getTable.execute(token));
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Order from the table; waits for a waiter to approve it")
    public TicketResponse order(@PathVariable UUID token, @Valid @RequestBody OrderRequest req) {
        return TicketResponse.from(placeOrder.byGuest(token, req.toLines()));
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Live status of this table's rounds (Server-Sent Events)")
    public SseEmitter stream(@PathVariable UUID token) {
        Long tableId = tables.findByQrToken(token).orElseThrow(TableNotFoundException::new).id();
        return hub.table(tenants.current().id(), tableId);
    }

    @GetMapping("/events")
    @Operation(summary = "The same events by polling, for proxies that buffer streams")
    public EventLog.Batch events(@PathVariable UUID token, @RequestParam(required = false) Long after) {
        Long tableId = tables.findByQrToken(token).orElseThrow(TableNotFoundException::new).id();
        return hub.tableSince(tenants.current().id(), tableId, after);
    }
}
