package com.lsoftware.restauco.infrastructure.floor.controller;

import com.lsoftware.restauco.infrastructure.floor.dto.*;
import com.lsoftware.restauco.infrastructure.order.dto.CancelRequest;
import com.lsoftware.restauco.infrastructure.order.dto.OrderRequest;
import com.lsoftware.restauco.infrastructure.order.dto.TicketResponse;
import com.lsoftware.restauco.usecase.floor.CloseSessionUseCase;
import com.lsoftware.restauco.usecase.floor.GetFloorUseCase;
import com.lsoftware.restauco.usecase.floor.ManagePaymentMethodUseCase;
import com.lsoftware.restauco.usecase.order.ChangeTicketUseCase;
import com.lsoftware.restauco.usecase.order.ListTicketsUseCase;
import com.lsoftware.restauco.usecase.order.PlaceOrderUseCase;
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
@RequestMapping("/api/panel/floor")
@RequiredArgsConstructor
@Tag(name = "Floor (waiter)", description = "Tables, orders and bills")
@SecurityRequirement(name = "bearerAuth")
public class FloorController {

    private final GetFloorUseCase floor;
    private final PlaceOrderUseCase placeOrder;
    private final ChangeTicketUseCase tickets;
    private final ListTicketsUseCase listTickets;
    private final CloseSessionUseCase closeSession;
    private final ManagePaymentMethodUseCase paymentMethods;

    @GetMapping("/tables")
    @Operation(summary = "Floor map: every table with its open bill and order status")
    public List<TableSummaryResponse> tables() {
        return floor.floor().stream().map(TableSummaryResponse::from).toList();
    }

    @GetMapping("/tables/{id}")
    @Operation(summary = "A table with its open bill and rounds")
    public TableDetailResponse table(@PathVariable Long id) {
        return TableDetailResponse.from(floor.table(id));
    }

    @GetMapping("/tickets")
    @Operation(summary = "Open tickets of every table: QR orders to approve, rounds in the kitchen, pickups")
    public List<TicketResponse> activeTickets() {
        return listTickets.active().stream().map(TicketResponse::from).toList();
    }

    @PostMapping("/tables/{id}/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Send a round to the kitchen (opens the bill if needed)")
    public TicketResponse order(@PathVariable Long id, @Valid @RequestBody OrderRequest req, @AuthenticationPrincipal Jwt jwt) {
        return TicketResponse.from(placeOrder.byWaiter(id, req.toLines(), jwt.getSubject()));
    }

    @PostMapping("/tickets/{id}/approve")
    @Operation(summary = "Send a QR order to the kitchen")
    public TicketResponse approve(@PathVariable Long id) {
        return TicketResponse.from(tickets.approve(id));
    }

    @PostMapping("/tickets/{id}/deliver")
    @Operation(summary = "Picked up from the kitchen and served")
    public TicketResponse deliver(@PathVariable Long id) {
        return TicketResponse.from(tickets.deliver(id));
    }

    @PostMapping("/tickets/{id}/cancel")
    @Operation(summary = "Cancel a round the kitchen has not started")
    public TicketResponse cancel(@PathVariable Long id, @Valid @RequestBody(required = false) CancelRequest req) {
        return TicketResponse.from(tickets.cancel(id, req != null ? req.reason() : null));
    }

    @PostMapping("/sessions/{id}/close")
    @Operation(summary = "Charge the bill (one method, or split across several) and free the table")
    public SessionResponse close(@PathVariable Long id, @Valid @RequestBody CloseSessionRequest req,
                                 @AuthenticationPrincipal Jwt jwt) {
        return SessionResponse.from(closeSession.execute(id, req.parts(), jwt.getSubject()));
    }

    @GetMapping("/payment-methods")
    @Operation(summary = "Payment methods the restaurant accepts")
    public List<PaymentMethodResponse> paymentMethods() {
        return paymentMethods.listActive().stream().map(PaymentMethodResponse::from).toList();
    }
}
