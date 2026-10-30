package com.lsoftware.restauco.usecase.delivery;

import com.lsoftware.restauco.entity.delivery.model.DeliverySettings;
import com.lsoftware.restauco.entity.floor.exception.PaymentMethodNotFoundException;
import com.lsoftware.restauco.entity.floor.gateway.PaymentMethodGateway;
import com.lsoftware.restauco.entity.floor.model.PaymentMethod;
import com.lsoftware.restauco.entity.tenant.gateway.TenantGateway;
import com.lsoftware.restauco.entity.tenant.model.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ManageDeliverySettingsUseCaseTest {

    private static final Tenant TOCHES = new Tenant(1L, "toches-arepas", "Toche's", ZoneId.of("America/Bogota"),
            null, null, Map.of(), DeliverySettings.OFF);

    private final TenantGateway tenants = mock(TenantGateway.class);
    private final PaymentMethodGateway methods = mock(PaymentMethodGateway.class);
    private final ManageDeliverySettingsUseCase useCase = new ManageDeliverySettingsUseCase(() -> TOCHES, tenants, methods);

    @BeforeEach
    void setUp() {
        when(methods.findAll()).thenReturn(List.of(new PaymentMethod(1L, "Efectivo", 1, true),
                new PaymentMethod(2L, "Nequi", 2, false)));
        when(tenants.saveDelivery(eq(1L), any())).thenAnswer(inv -> new Tenant(1L, "toches-arepas", "Toche's",
                ZoneId.of("America/Bogota"), null, null, Map.of(), inv.getArgument(1)));
    }

    @Test
    void anActiveMethodCanBeTheOnlyOneForDelivery() {
        DeliverySettings saved = useCase.update(zone(List.of(1L)));

        assertThat(saved.paymentMethodIds()).containsExactly(1L);
    }

    @Test
    void anInactiveMethodIsRejected() {
        assertThatThrownBy(() -> useCase.update(zone(List.of(2L))))
                .isInstanceOf(PaymentMethodNotFoundException.class);
        verify(tenants, never()).saveDelivery(any(), any());
    }

    @Test
    void anUnknownMethodIsRejected() {
        assertThatThrownBy(() -> useCase.update(zone(List.of(1L, 99L))))
                .isInstanceOf(PaymentMethodNotFoundException.class);
    }

    private static DeliverySettings zone(List<Long> paymentMethodIds) {
        return new DeliverySettings(true, 3.4307, -76.5428, 4, 3000, 10000, paymentMethodIds);
    }
}
