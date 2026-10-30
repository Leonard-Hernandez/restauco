package com.lsoftware.restauco.usecase.order;

import com.lsoftware.restauco.entity.floor.exception.TableNotFoundException;
import com.lsoftware.restauco.entity.floor.gateway.SessionGateway;
import com.lsoftware.restauco.entity.floor.gateway.TableGateway;
import com.lsoftware.restauco.entity.floor.model.DiningTable;
import com.lsoftware.restauco.entity.order.gateway.TicketGateway;
import com.lsoftware.restauco.usecase.order.dto.GuestTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetGuestTableUseCase {

    private final TableGateway tables;
    private final SessionGateway sessions;
    private final TicketGateway tickets;

    @Transactional(readOnly = true)
    public GuestTable execute(UUID qrToken) {
        DiningTable table = tables.findByQrToken(qrToken).orElseThrow(TableNotFoundException::new);
        return new GuestTable(table, sessions.findOpenByTable(table.id())
                .map(session -> tickets.findBySession(session.id()))
                .orElse(List.of()));
    }
}
