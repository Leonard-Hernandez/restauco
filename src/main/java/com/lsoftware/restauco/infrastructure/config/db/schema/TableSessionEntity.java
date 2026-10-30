package com.lsoftware.restauco.infrastructure.config.db.schema;

import jakarta.persistence.*;
import com.lsoftware.restauco.entity.floor.model.Payment;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TenantId;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "table_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    /** Null for a delivery */
    @Column(name = "table_id")
    private Long tableId;

    @Column(nullable = false, length = 10)
    private String status;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private OffsetDateTime openedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "closed_by", length = 64)
    private String closedBy;

    @Column(name = "payment_method_id")
    private Long paymentMethodId;

    @Column(name = "payment_method_name", length = 40)
    private String paymentMethodName;

    private Long total;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<Payment> payments;

    @Column(name = "delivery_fee", nullable = false)
    private long deliveryFee;
}
