package com.lsoftware.restauco.infrastructure.config.db.mapper;

import com.lsoftware.restauco.entity.order.model.OrderTicket;
import com.lsoftware.restauco.entity.order.model.TicketSource;
import com.lsoftware.restauco.entity.order.model.TicketStatus;
import com.lsoftware.restauco.infrastructure.config.db.schema.OrderTicketEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class OrderTicketMapper {

    public OrderTicket toDomain(OrderTicketEntity e) {
        return new OrderTicket(e.getId(), e.getSessionId(), e.getTableId(), e.getTableNumber(), e.getDeliveryId(),
                TicketStatus.valueOf(e.getStatus()), TicketSource.valueOf(e.getSource()), e.getWaiterSub(),
                e.getItems(), e.getTotal(), e.getCreatedAt(), e.getSentAt(), e.getStartedAt(), e.getReadyAt(),
                e.getDeliveredAt(), e.getCancelledAt(), e.getCancelReason(), e.getVersion());
    }

    public OrderTicketEntity toEntity(OrderTicket t) {
        return OrderTicketEntity.builder()
                .id(t.id())
                .sessionId(t.sessionId())
                .tableId(t.tableId())
                .tableNumber(t.tableNumber())
                .deliveryId(t.deliveryId())
                .status(t.status().name())
                .source(t.source().name())
                .waiterSub(t.waiterSub())
                .items(new ArrayList<>(t.items()))
                .total(t.total())
                .createdAt(t.createdAt())
                .sentAt(t.sentAt())
                .startedAt(t.startedAt())
                .readyAt(t.readyAt())
                .deliveredAt(t.deliveredAt())
                .cancelledAt(t.cancelledAt())
                .cancelReason(t.cancelReason())
                .version(t.version())
                .build();
    }
}
