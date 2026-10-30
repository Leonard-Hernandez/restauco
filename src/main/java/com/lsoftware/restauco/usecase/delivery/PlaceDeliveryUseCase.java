package com.lsoftware.restauco.usecase.delivery;

import com.lsoftware.restauco.entity.delivery.exception.DeliveryClosedException;
import com.lsoftware.restauco.entity.delivery.exception.InvalidDeliveryException;
import com.lsoftware.restauco.entity.delivery.exception.OutOfZoneException;
import com.lsoftware.restauco.entity.delivery.gateway.DeliveryGateway;
import com.lsoftware.restauco.entity.delivery.model.DeliveryOrder;
import com.lsoftware.restauco.entity.delivery.model.DeliverySettings;
import com.lsoftware.restauco.entity.delivery.model.Place;
import com.lsoftware.restauco.entity.exception.TooManyRequestsException;
import com.lsoftware.restauco.entity.floor.exception.PaymentMethodNotFoundException;
import com.lsoftware.restauco.entity.floor.gateway.PaymentMethodGateway;
import com.lsoftware.restauco.entity.floor.gateway.SessionGateway;
import com.lsoftware.restauco.entity.floor.model.PaymentMethod;
import com.lsoftware.restauco.entity.floor.model.TableSession;
import com.lsoftware.restauco.entity.order.gateway.OrderEventGateway;
import com.lsoftware.restauco.entity.order.gateway.TicketGateway;
import com.lsoftware.restauco.entity.order.model.OrderTicket;
import com.lsoftware.restauco.entity.order.model.TicketItem;
import com.lsoftware.restauco.entity.tenant.gateway.CurrentTenantGateway;
import com.lsoftware.restauco.usecase.delivery.dto.DeliveryCommand;
import com.lsoftware.restauco.usecase.delivery.dto.DeliveryView;
import com.lsoftware.restauco.usecase.order.OrderPricer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * A delivery order from the website. Everything is checked here, never trusted from the page:
 * the pin must be a real street address on the map and inside the delivery zone, the phone
 * a Colombian number, prices come from the menu. It then waits for a waiter, who sees the
 * address next to what the map found there and calls if in doubt, before the kitchen gets it.
 */
@Component
public class PlaceDeliveryUseCase {

    private final LocateDeliveryUseCase locate;
    private final OrderPricer pricer;
    private final PaymentMethodGateway paymentMethods;
    private final SessionGateway sessions;
    private final DeliveryGateway deliveries;
    private final TicketGateway tickets;
    private final OrderEventGateway events;
    private final CurrentTenantGateway tenants;
    private final TransactionOperations transactions;
    private final Clock clock;
    private final int maxPerPhonePerHour;

    public PlaceDeliveryUseCase(LocateDeliveryUseCase locate, OrderPricer pricer, PaymentMethodGateway paymentMethods,
                                SessionGateway sessions, DeliveryGateway deliveries, TicketGateway tickets,
                                OrderEventGateway events, CurrentTenantGateway tenants, TransactionOperations transactions,
                                Clock clock, @Value("${restauco.delivery.max-per-phone-per-hour:3}") int maxPerPhonePerHour) {
        this.locate = locate;
        this.pricer = pricer;
        this.paymentMethods = paymentMethods;
        this.sessions = sessions;
        this.deliveries = deliveries;
        this.tickets = tickets;
        this.events = events;
        this.tenants = tenants;
        this.transactions = transactions;
        this.clock = clock;
        this.maxPerPhonePerHour = maxPerPhonePerHour;
    }

    // Not one transaction: the map lookup is a slow outside call and must not hold a DB connection
    public DeliveryView execute(DeliveryCommand cmd) {
        DeliverySettings settings = tenants.current().delivery();
        // A method chosen for delivery and deactivated later can leave no way to pay: closed, like the page shows
        if (!settings.open() || paymentMethods.findAll().stream().noneMatch(m -> m.active() && settings.accepts(m.id()))) {
            throw new DeliveryClosedException();
        }

        String name = required(cmd.customerName(), 80, "NAME_REQUIRED", "Write your name");
        String phone = phone(cmd.phone());
        String address = required(cmd.address(), 160, "ADDRESS_REQUIRED", "Write the street and number");
        if (address.length() < 5) throw new InvalidDeliveryException("ADDRESS_REQUIRED", "Write the street and number");
        String detail = optional(cmd.addressDetail(), 120);

        int distance = settings.distanceTo(cmd.lat(), cmd.lng());
        if (!settings.reaches(distance)) throw new OutOfZoneException();

        List<TicketItem> items = pricer.price(cmd.lines());
        long subtotal = items.stream().mapToLong(TicketItem::lineTotal).sum();
        if (subtotal < settings.minOrder()) {
            throw new InvalidDeliveryException("BELOW_MINIMUM", "Delivery orders start at " + settings.minOrder());
        }
        PaymentMethod method = cmd.paymentMethodId() == null ? null : paymentMethods.findById(cmd.paymentMethodId())
                .filter(PaymentMethod::active).filter(m -> settings.accepts(m.id())).orElse(null);
        if (method == null) throw new PaymentMethodNotFoundException();
        if (cmd.cashFor() != null && cmd.cashFor() < subtotal + settings.fee()) {
            throw new InvalidDeliveryException("CASH_TOO_LOW", "The cash must cover the total");
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        if (deliveries.countByPhoneSince(phone, now.minusHours(1)) >= maxPerPhonePerHour) {
            throw new TooManyRequestsException("TOO_MANY_DELIVERIES", "This phone already ordered several times this hour");
        }

        Place place = locate.verify(cmd.lat(), cmd.lng());
        boolean matches = AddressText.sameStreet(address, place.road());

        return transactions.execute(tx -> {
            TableSession session = sessions.save(TableSession.openDelivery(settings.fee(), now));
            DeliveryOrder delivery = deliveries.save(new DeliveryOrder(null, session.id(), UUID.randomUUID(), name, phone,
                    address, detail, cmd.lat(), cmd.lng(), place.label(), matches, distance, method.id(), method.name(),
                    cmd.cashFor(), now));
            OrderTicket ticket = tickets.save(OrderTicket.forDelivery(session.id(), delivery.id(), items, now));
            events.ticketChanged(ticket);
            events.sessionChanged(null);
            return new DeliveryView(delivery, session, List.of(ticket));
        });
    }

    /** Colombian mobile (3xx xxx xxxx) or landline (60x xxx xxxx), with or without +57. */
    static String phone(String raw) {
        String digits = raw == null ? "" : raw.replaceAll("\\D", "");
        if (digits.length() == 12 && digits.startsWith("57")) digits = digits.substring(2);
        if (!digits.matches("3\\d{9}|60\\d{8}")) {
            throw new InvalidDeliveryException("INVALID_PHONE", "Write a Colombian phone number (10 digits)");
        }
        return digits;
    }

    private static String required(String value, int max, String code, String message) {
        String clean = optional(value, max);
        if (clean == null) throw new InvalidDeliveryException(code, message);
        return clean;
    }

    private static String optional(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String clean = value.strip().replaceAll("\\s+", " ");
        return clean.length() > max ? clean.substring(0, max) : clean;
    }
}
