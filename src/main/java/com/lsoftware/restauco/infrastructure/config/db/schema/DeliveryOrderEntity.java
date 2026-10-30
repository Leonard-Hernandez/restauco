package com.lsoftware.restauco.infrastructure.config.db.schema;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.TenantId;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "delivery_order")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(name = "session_id", nullable = false, updatable = false)
    private Long sessionId;

    @Column(nullable = false, updatable = false)
    private UUID token;

    @Column(name = "customer_name", nullable = false, length = 80)
    private String customerName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 160)
    private String address;

    @Column(name = "address_detail", length = 120)
    private String addressDetail;

    @Column(nullable = false)
    private double lat;

    @Column(nullable = false)
    private double lng;

    @Column(name = "place_label", nullable = false, length = 255)
    private String placeLabel;

    @Column(name = "matches_address", nullable = false)
    private boolean matchesAddress;

    @Column(name = "distance_m", nullable = false)
    private int distanceMeters;

    @Column(name = "payment_method_id")
    private Long paymentMethodId;

    @Column(name = "payment_method_name", length = 40)
    private String paymentMethodName;

    @Column(name = "cash_for")
    private Long cashFor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
