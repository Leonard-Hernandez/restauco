package com.lsoftware.restauco.infrastructure.config.db.schema;

import com.lsoftware.restauco.entity.order.model.TicketItem;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TenantId;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "order_ticket")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderTicketEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(name = "session_id", nullable = false, updatable = false)
    private Long sessionId;

    /** Null for a delivery */
    @Column(name = "table_id", updatable = false)
    private Long tableId;

    @Column(name = "table_number", updatable = false)
    private Integer tableNumber;

    @Column(name = "delivery_id", updatable = false)
    private Long deliveryId;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false, length = 10, updatable = false)
    private String source;

    @Column(name = "waiter_sub", length = 64)
    private String waiterSub;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb", updatable = false)
    private List<TicketItem> items;

    @Column(nullable = false, updatable = false)
    private long total;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "ready_at")
    private OffsetDateTime readyAt;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "cancel_reason", length = 140)
    private String cancelReason;

    @Version
    private Long version;
}
