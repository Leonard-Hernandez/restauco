package com.lsoftware.restauco.usecase.order;

import com.lsoftware.restauco.entity.order.gateway.TicketGateway;
import com.lsoftware.restauco.entity.order.model.OrderTicket;
import com.lsoftware.restauco.entity.order.model.TicketStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;

/** Open tickets across the restaurant, oldest first. */
@Component
@RequiredArgsConstructor
public class ListTicketsUseCase {

    private final TicketGateway tickets;

    /** The kitchen board: new, in preparation and ready for pickup. */
    @Transactional(readOnly = true)
    public List<OrderTicket> kitchen() {
        return tickets.findByStatusIn(TicketStatus.KITCHEN);
    }

    /** The floor: everything not yet delivered or cancelled (QR orders to approve, pickups...). */
    @Transactional(readOnly = true)
    public List<OrderTicket> active() {
        return tickets.findByStatusIn(EnumSet.of(TicketStatus.PENDING_APPROVAL, TicketStatus.SENT,
                TicketStatus.PREPARING, TicketStatus.READY));
    }
}
