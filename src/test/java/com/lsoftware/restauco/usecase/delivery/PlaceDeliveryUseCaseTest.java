package com.lsoftware.restauco.usecase.delivery;

import com.lsoftware.restauco.entity.delivery.exception.DeliveryClosedException;
import com.lsoftware.restauco.entity.delivery.exception.OutOfZoneException;
import com.lsoftware.restauco.entity.delivery.exception.PlaceNotFoundException;
import com.lsoftware.restauco.entity.delivery.gateway.DeliveryGateway;
import com.lsoftware.restauco.entity.delivery.gateway.GeocodingGateway;
import com.lsoftware.restauco.entity.delivery.model.DeliveryOrder;
import com.lsoftware.restauco.entity.delivery.model.DeliverySettings;
import com.lsoftware.restauco.entity.delivery.model.Place;
import com.lsoftware.restauco.entity.delivery.exception.InvalidDeliveryException;
import com.lsoftware.restauco.entity.floor.exception.PaymentMethodNotFoundException;
import com.lsoftware.restauco.entity.floor.gateway.PaymentMethodGateway;
import com.lsoftware.restauco.entity.floor.gateway.SessionGateway;
import com.lsoftware.restauco.entity.floor.model.PaymentMethod;
import com.lsoftware.restauco.entity.floor.model.TableSession;
import com.lsoftware.restauco.entity.menu.gateway.DishGateway;
import com.lsoftware.restauco.entity.menu.model.Dish;
import com.lsoftware.restauco.entity.order.gateway.OrderEventGateway;
import com.lsoftware.restauco.entity.order.gateway.TicketGateway;
import com.lsoftware.restauco.entity.order.model.OrderTicket;
import com.lsoftware.restauco.entity.order.model.TicketSource;
import com.lsoftware.restauco.entity.order.model.TicketStatus;
import com.lsoftware.restauco.entity.tenant.model.Tenant;
import com.lsoftware.restauco.usecase.delivery.dto.DeliveryCommand;
import com.lsoftware.restauco.usecase.delivery.dto.DeliveryView;
import com.lsoftware.restauco.usecase.order.OrderPricer;
import com.lsoftware.restauco.usecase.order.dto.OrderLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

class PlaceDeliveryUseCaseTest {

    // Toche's, San Fernando (Cali); 4 km zone, 3.000 delivery
    private static final DeliverySettings ZONE = new DeliverySettings(true, 3.4307, -76.5428, 4, 3000, 10000);
    private static final Tenant TOCHES = new Tenant(1L, "toches-arepas", "Toche's", ZoneId.of("America/Bogota"),
            null, null, Map.of(), ZONE);
    private static final Dish SIFRINA = new Dish(2L, 2L, "Arepa Sifrina", null, 15000, null, true, true, 1,
            List.of(), List.of(), List.of());
    // About 300 m away, on Calle 5B
    private static final double LAT = 3.4312;
    private static final double LNG = -76.5450;

    private final GeocodingGateway geocoder = mock(GeocodingGateway.class);
    private final DishGateway dishes = mock(DishGateway.class);
    private final PaymentMethodGateway methods = mock(PaymentMethodGateway.class);
    private final SessionGateway sessions = mock(SessionGateway.class);
    private final DeliveryGateway deliveries = mock(DeliveryGateway.class);
    private final TicketGateway tickets = mock(TicketGateway.class);
    private final OrderEventGateway events = mock(OrderEventGateway.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-26T23:00:00Z"), ZoneOffset.UTC);
    private final PlaceDeliveryUseCase useCase = new PlaceDeliveryUseCase(
            new LocateDeliveryUseCase(geocoder, () -> TOCHES), new OrderPricer(dishes), methods, sessions, deliveries,
            tickets, events, () -> TOCHES, TransactionOperations.withoutTransaction(), clock, 3);

    @BeforeEach
    void setUp() {
        when(dishes.findAllById(any())).thenReturn(List.of(SIFRINA));
        when(methods.findById(1L)).thenReturn(Optional.of(new PaymentMethod(1L, "Efectivo", 1, true)));
        when(methods.findAll()).thenReturn(List.of(new PaymentMethod(1L, "Efectivo", 1, true),
                new PaymentMethod(2L, "Nequi", 2, true)));
        when(geocoder.reverse(anyDouble(), anyDouble()))
                .thenReturn(Optional.of(new Place(LAT, LNG, "Calle 5B", null, "San Fernando", "Cali")));
        when(sessions.save(any())).thenAnswer(inv -> {
            TableSession s = inv.getArgument(0);
            return new TableSession(70L, null, s.status(), s.openedAt(), null, null, null, null, null, null, s.deliveryFee());
        });
        when(deliveries.save(any())).thenAnswer(inv -> {
            DeliveryOrder d = inv.getArgument(0);
            return new DeliveryOrder(9L, d.sessionId(), d.token(), d.customerName(), d.phone(), d.address(),
                    d.addressDetail(), d.lat(), d.lng(), d.placeLabel(), d.matchesAddress(), d.distanceMeters(),
                    d.paymentMethodId(), d.paymentMethodName(), d.cashFor(), d.createdAt());
        });
        when(tickets.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void aVerifiedAddressWaitsForTheWaiter() {
        DeliveryView view = useCase.execute(order("Cl. 5B # 36-20", LAT, LNG, "+57 300 123 4567"));

        OrderTicket ticket = view.tickets().getFirst();
        assertThat(ticket.status()).isEqualTo(TicketStatus.PENDING_APPROVAL);
        assertThat(ticket.source()).isEqualTo(TicketSource.DELIVERY);
        assertThat(ticket.deliveryId()).isEqualTo(9L);
        assertThat(ticket.tableId()).isNull();
        assertThat(view.delivery().phone()).isEqualTo("3001234567");
        assertThat(view.delivery().matchesAddress()).isTrue();
        assertThat(view.delivery().placeLabel()).isEqualTo("Calle 5B, San Fernando, Cali");
        assertThat(view.total()).isEqualTo(2 * 15000 + 3000);
        verify(events).ticketChanged(ticket);
    }

    @Test
    void aPinTheMapHasNoAddressForIsRejected() {
        when(geocoder.reverse(anyDouble(), anyDouble())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(order("Cl. 5B # 36-20", LAT, LNG, "3001234567")))
                .isInstanceOf(PlaceNotFoundException.class);
        verify(tickets, never()).save(any());
    }

    @Test
    void outsideTheZoneIsRejectedWithoutAskingTheMap() {
        // Jamundí, ~15 km south
        assertThatThrownBy(() -> useCase.execute(order("Calle 10 # 5-20", 3.2620, -76.5390, "3001234567")))
                .isInstanceOf(OutOfZoneException.class);
        verifyNoInteractions(geocoder);
    }

    @Test
    void aPhoneThatIsNotColombianIsRejected() {
        assertThatThrownBy(() -> useCase.execute(order("Cl. 5B # 36-20", LAT, LNG, "12345")))
                .isInstanceOf(InvalidDeliveryException.class)
                .hasFieldOrPropertyWithValue("code", "INVALID_PHONE");
    }

    @Test
    void aWrittenStreetThatIsNotUnderThePinIsFlaggedForTheWaiter() {
        DeliveryView view = useCase.execute(order("Calle 9 # 44-35", LAT, LNG, "3001234567"));

        assertThat(view.delivery().matchesAddress()).isFalse();
    }

    @Test
    void tooManyOrdersFromOnePhoneAreStopped() {
        when(deliveries.countByPhoneSince(eq("3001234567"), any())).thenReturn(3L);

        assertThatThrownBy(() -> useCase.execute(order("Cl. 5B # 36-20", LAT, LNG, "3001234567")))
                .hasFieldOrPropertyWithValue("code", "TOO_MANY_DELIVERIES");
    }

    @Test
    void aPaymentMethodTheOwnerDoesNotTakeForDeliveryIsRejected() {
        DeliverySettings noCash = new DeliverySettings(true, 3.4307, -76.5428, 4, 5000, 0, List.of(2L));
        Tenant tenant = new Tenant(1L, "toches-arepas", "Toche's", ZoneId.of("America/Bogota"), null, null, Map.of(), noCash);
        PlaceDeliveryUseCase strict = new PlaceDeliveryUseCase(new LocateDeliveryUseCase(geocoder, () -> tenant),
                new OrderPricer(dishes), methods, sessions, deliveries, tickets, events, () -> tenant,
                TransactionOperations.withoutTransaction(), clock, 3);

        assertThatThrownBy(() -> strict.execute(order("Cl. 5B # 36-20", LAT, LNG, "3001234567")))
                .isInstanceOf(PaymentMethodNotFoundException.class);
    }

    @Test
    void aDeliveryWhoseOnlyMethodsWereDeactivatedIsClosed() {
        DeliverySettings nequiOnly = new DeliverySettings(true, 3.4307, -76.5428, 4, 5000, 0, List.of(2L));
        Tenant tenant = new Tenant(1L, "toches-arepas", "Toche's", ZoneId.of("America/Bogota"), null, null, Map.of(), nequiOnly);
        when(methods.findAll()).thenReturn(List.of(new PaymentMethod(1L, "Efectivo", 1, true),
                new PaymentMethod(2L, "Nequi", 2, false)));
        PlaceDeliveryUseCase strict = new PlaceDeliveryUseCase(new LocateDeliveryUseCase(geocoder, () -> tenant),
                new OrderPricer(dishes), methods, sessions, deliveries, tickets, events, () -> tenant,
                TransactionOperations.withoutTransaction(), clock, 3);

        assertThatThrownBy(() -> strict.execute(order("Cl. 5B # 36-20", LAT, LNG, "3001234567")))
                .isInstanceOf(DeliveryClosedException.class);
        verifyNoInteractions(geocoder);
    }

    private static DeliveryCommand order(String address, double lat, double lng, String phone) {
        return new DeliveryCommand("Ana", phone, address, null, lat, lng, 1L, null,
                List.of(new OrderLine(2L, 2, null, null, null)));
    }
}
