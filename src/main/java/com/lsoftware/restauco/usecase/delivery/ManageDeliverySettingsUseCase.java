package com.lsoftware.restauco.usecase.delivery;

import com.lsoftware.restauco.entity.delivery.exception.InvalidDeliveryException;
import com.lsoftware.restauco.entity.delivery.model.DeliverySettings;
import com.lsoftware.restauco.entity.floor.exception.PaymentMethodNotFoundException;
import com.lsoftware.restauco.entity.floor.gateway.PaymentMethodGateway;
import com.lsoftware.restauco.entity.floor.model.PaymentMethod;
import com.lsoftware.restauco.entity.tenant.gateway.CurrentTenantGateway;
import com.lsoftware.restauco.entity.tenant.gateway.TenantGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/** The owner turns delivery on or off and sets the zone, fee, minimum order and accepted payment methods. */
@Component
@RequiredArgsConstructor
public class ManageDeliverySettingsUseCase {

    static final double MAX_RADIUS_KM = 30;

    private final CurrentTenantGateway current;
    private final TenantGateway tenants;
    private final PaymentMethodGateway paymentMethods;

    public DeliverySettings get() {
        return current.current().delivery();
    }

    @Transactional
    public DeliverySettings update(DeliverySettings settings) {
        if (settings.enabled() && (settings.lat() == null || settings.lng() == null)) {
            throw new InvalidDeliveryException("INVALID_LOCATION", "Set where the restaurant is before opening delivery");
        }
        if (settings.lat() != null && (Math.abs(settings.lat()) > 90 || settings.lng() == null || Math.abs(settings.lng()) > 180)) {
            throw new InvalidDeliveryException("INVALID_LOCATION", "That is not a place on the map");
        }
        if (settings.radiusKm() <= 0 || settings.radiusKm() > MAX_RADIUS_KM) {
            throw new InvalidDeliveryException("INVALID_RADIUS", "The zone must be between 0 and " + MAX_RADIUS_KM + " km");
        }
        if (settings.fee() < 0 || settings.minOrder() < 0) {
            throw new InvalidDeliveryException("INVALID_FEE", "Amounts cannot be negative");
        }
        // Only active methods: an inactive one in the list would narrow delivery to a way nobody can pay
        Set<Long> usable = paymentMethods.findAll().stream().filter(PaymentMethod::active).map(PaymentMethod::id)
                .collect(Collectors.toSet());
        if (!usable.containsAll(settings.paymentMethodIds())) throw new PaymentMethodNotFoundException();
        return tenants.saveDelivery(current.current().id(), settings).delivery();
    }
}
