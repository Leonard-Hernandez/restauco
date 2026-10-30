package com.lsoftware.restauco.infrastructure.order.controller;

import com.lsoftware.restauco.infrastructure.order.dto.TicketResponse;
import com.lsoftware.restauco.usecase.order.ChangeTicketUseCase;
import com.lsoftware.restauco.usecase.order.ListTicketsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/panel/kitchen/tickets")
@RequiredArgsConstructor
@Tag(name = "Kitchen", description = "Kitchen board")
@SecurityRequirement(name = "bearerAuth")
public class KitchenController {

    private final ListTicketsUseCase list;
    private final ChangeTicketUseCase tickets;

    @GetMapping
    @Operation(summary = "New, in preparation and ready tickets, oldest first")
    public List<TicketResponse> board() {
        return list.kitchen().stream().map(TicketResponse::from).toList();
    }

    @PostMapping("/{id}/start")
    public TicketResponse start(@PathVariable Long id) {
        return TicketResponse.from(tickets.start(id));
    }

    @PostMapping("/{id}/ready")
    @Operation(summary = "Ready for pickup: the waiter is notified")
    public TicketResponse ready(@PathVariable Long id) {
        return TicketResponse.from(tickets.ready(id));
    }
}
