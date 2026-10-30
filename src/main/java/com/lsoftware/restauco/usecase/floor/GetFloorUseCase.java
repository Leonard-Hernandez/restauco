package com.lsoftware.restauco.usecase.floor;

import com.lsoftware.restauco.entity.floor.exception.TableNotFoundException;
import com.lsoftware.restauco.entity.floor.gateway.SessionGateway;
import com.lsoftware.restauco.entity.floor.gateway.TableGateway;
import com.lsoftware.restauco.entity.floor.model.DiningTable;
import com.lsoftware.restauco.entity.floor.model.TableSession;
import com.lsoftware.restauco.entity.order.gateway.TicketGateway;
import com.lsoftware.restauco.entity.order.model.OrderTicket;
import com.lsoftware.restauco.entity.order.model.TicketStatus;
import com.lsoftware.restauco.usecase.floor.dto.TableDetail;
import com.lsoftware.restauco.usecase.floor.dto.TableSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** What the waiter sees: every table with its open bill and how its orders are doing. */
@Component
@RequiredArgsConstructor
public class GetFloorUseCase {

    private final TableGateway tables;
    private final SessionGateway sessions;
    private final TicketGateway tickets;

    @Transactional(readOnly = true)
    public List<TableSummary> floor() {
        Map<Long, TableSession> open = sessions.findAllOpen().stream()
                .collect(Collectors.toMap(TableSession::tableId, Function.identity()));
        Map<Long, List<OrderTicket>> bySession = open.isEmpty() ? Map.of()
                : tickets.findBySessions(open.values().stream().map(TableSession::id).toList()).stream()
                        .collect(Collectors.groupingBy(OrderTicket::sessionId));

        return tables.findAll().stream()
                .filter(t -> t.active() || open.containsKey(t.id()))
                .map(table -> {
                    TableSession session = open.get(table.id());
                    List<OrderTicket> rounds = session != null ? bySession.getOrDefault(session.id(), List.of()) : List.of();
                    return new TableSummary(table, session,
                            count(rounds, TicketStatus.PENDING_APPROVAL),
                            count(rounds, TicketStatus.SENT) + count(rounds, TicketStatus.PREPARING),
                            count(rounds, TicketStatus.READY),
                            rounds.stream().mapToLong(OrderTicket::billable).sum());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public TableDetail table(Long tableId) {
        DiningTable table = tables.findById(tableId).orElseThrow(TableNotFoundException::new);
        return detail(table);
    }

    TableDetail detail(DiningTable table) {
        return sessions.findOpenByTable(table.id())
                .map(session -> TableDetail.of(table, session, tickets.findBySession(session.id())))
                .orElseGet(() -> TableDetail.of(table, null, List.of()));
    }

    private static int count(List<OrderTicket> rounds, TicketStatus status) {
        return (int) rounds.stream().filter(t -> t.status() == status).count();
    }
}
