package com.lsoftware.restauco.infrastructure.delivery.dto;

import com.lsoftware.restauco.entity.delivery.model.DeliverySettings;
import com.lsoftware.restauco.entity.floor.model.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryInfoResponseTest {

    private static final List<PaymentMethod> ACTIVE = List.of(new PaymentMethod(1L, "Efectivo", 1, true));

    @Test
    void onlyTheMethodsTakenForDeliveryAreShown() {
        DeliveryInfoResponse info = DeliveryInfoResponse.from(zone(List.of()), ACTIVE);

        assertThat(info.open()).isTrue();
        assertThat(info.paymentMethods()).extracting(DeliveryInfoResponse.Method::name).containsExactly("Efectivo");
    }

    @Test
    void withNoActiveMethodLeftForDeliveryItShowsClosed() {
        // Nequi (2) was the only one for delivery and has been deactivated since
        DeliveryInfoResponse info = DeliveryInfoResponse.from(zone(List.of(2L)), ACTIVE);

        assertThat(info.open()).isFalse();
        assertThat(info.paymentMethods()).isEmpty();
    }

    private static DeliverySettings zone(List<Long> paymentMethodIds) {
        return new DeliverySettings(true, 3.4307, -76.5428, 4, 3000, 10000, paymentMethodIds);
    }
}
