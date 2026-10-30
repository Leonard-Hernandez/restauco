package com.lsoftware.restauco.infrastructure.mcp;

import com.lsoftware.restauco.entity.delivery.model.DeliveryOrder;
import com.lsoftware.restauco.entity.floor.exception.TableNotFoundException;
import com.lsoftware.restauco.entity.member.model.MemberRole;
import com.lsoftware.restauco.entity.menu.model.Category;
import com.lsoftware.restauco.entity.menu.model.Dish;
import com.lsoftware.restauco.entity.menu.model.DishIngredient;
import com.lsoftware.restauco.entity.menu.model.Ingredient;
import com.lsoftware.restauco.entity.menu.model.ModifierGroup;
import com.lsoftware.restauco.entity.menu.model.ModifierOption;
import com.lsoftware.restauco.entity.order.model.OrderTicket;
import com.lsoftware.restauco.entity.order.model.TicketItem;
import com.lsoftware.restauco.entity.order.model.TicketItemModifier;
import com.lsoftware.restauco.entity.report.model.ReportPeriod;
import com.lsoftware.restauco.infrastructure.delivery.dto.DeliveryStatus;
import com.lsoftware.restauco.usecase.delivery.GetDeliveryUseCase;
import com.lsoftware.restauco.usecase.delivery.dto.DeliveryView;
import com.lsoftware.restauco.usecase.floor.GetFloorUseCase;
import com.lsoftware.restauco.usecase.floor.dto.TableDetail;
import com.lsoftware.restauco.usecase.floor.dto.TableSummary;
import com.lsoftware.restauco.usecase.menu.GetMenuUseCase;
import com.lsoftware.restauco.usecase.menu.ManageDishUseCase;
import com.lsoftware.restauco.usecase.menu.ManageIngredientUseCase;
import com.lsoftware.restauco.usecase.menu.dto.MenuView;
import com.lsoftware.restauco.usecase.order.ListTicketsUseCase;
import com.lsoftware.restauco.usecase.report.GetSalesReportUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.lsoftware.restauco.entity.member.model.MemberRole.*;

/**
 * The tools of the restaurant's assistant. They call the same use cases as the panel, so the
 * numbers match what the owner sees on /panel/estadisticas. Everything is read-only except
 * marking a dish or an ingredient sold out, which the kitchen can do from its screen too.
 */
@Component
@RequiredArgsConstructor
public class RestaurantTools {

    private static final Set<MemberRole> EVERYONE = Set.of(OWNER, WAITER, COOK);

    private final GetSalesReportUseCase sales;
    private final GetFloorUseCase floor;
    private final ListTicketsUseCase tickets;
    private final GetMenuUseCase menu;
    private final ManageDishUseCase dishes;
    private final ManageIngredientUseCase ingredients;
    private final GetDeliveryUseCase deliveries;

    public List<McpTool> tools() {
        return List.of(
                new McpTool("get_sales_report", "Ventas",
                        "Sales of a period, counted back from today in the restaurant's timezone (TODAY, WEEK = 7 days, "
                                + "MONTH = 30 days): revenue, bills, average ticket, the same numbers for the period before "
                                + "(cut at the same time of day), revenue by hour or day, by payment method, by waiter, best-selling "
                                + "dishes and kitchen times. Amounts are whole Colombian pesos.",
                        Schemas.object().enumeration("period", "Period; default TODAY",
                                Arrays.stream(ReportPeriod.values()).map(Enum::name).toList(), false).build(),
                        Set.of(OWNER), true,
                        (a, member) -> sales.execute(period(a.string("period")))),
                new McpTool("get_floor", "Mesas",
                        "Every table right now: free or with an open bill, since when, the bill so far, and how many rounds "
                                + "are waiting for approval (QR), in the kitchen, or ready to pick up.",
                        Schemas.object().build(),
                        Set.of(OWNER, WAITER), true,
                        (a, member) -> floor.floor().stream().map(RestaurantTools::table).toList()),
                new McpTool("get_table", "Cuenta de una mesa",
                        "The open bill of one table by its number, with every round (items, options, notes, status).",
                        Schemas.object().integer("table_number", "The number painted on the table", true).build(),
                        Set.of(OWNER, WAITER), true,
                        (a, member) -> tableDetail(a.requiredId("table_number"))),
                new McpTool("list_active_orders", "Pedidos en curso",
                        "Rounds not delivered yet, oldest first: QR orders waiting for a waiter, sent to the kitchen, "
                                + "being prepared, or ready to pick up, with their items and times.",
                        Schemas.object().build(),
                        EVERYONE, true,
                        (a, member) -> tickets.active().stream().map(RestaurantTools::ticket).toList()),
                new McpTool("get_menu", "Carta",
                        "The menu by category: dish ids, prices, whether each is sold out, ingredients that can be removed "
                                + "and the options to choose (e.g. the fillings of an arepa).",
                        Schemas.object().build(),
                        EVERYONE, true,
                        (a, member) -> menu(menu.published())),
                new McpTool("set_dish_availability", "Marcar agotado o disponible",
                        "Marks a dish as sold out (available=false) or back on sale (available=true). Guests and waiters "
                                + "see it at once. Look the dish id up with get_menu and confirm with the user first.",
                        Schemas.object()
                                .integer("dish_id", "Dish id (from get_menu)", true)
                                .bool("available", "false = sold out, true = on sale again", true)
                                .build(),
                        Set.of(OWNER, COOK), false,
                        (a, member) -> {
                            Dish d = dishes.setAvailability(a.requiredId("dish_id"), a.requiredBool("available"));
                            return Map.of("id", d.id(), "name", d.name(), "available", d.available());
                        }),
                new McpTool("set_ingredient_availability", "Marcar ingrediente agotado o disponible",
                        "Marks an ingredient as run out (available=false) or back (available=true). Dishes that cannot go "
                                + "without it stop being orderable, dishes that can are served without it, and options made "
                                + "with it (fillings) cannot be chosen. Look the id up with get_menu and confirm with the user first.",
                        Schemas.object()
                                .integer("ingredient_id", "Ingredient id (from get_menu)", true)
                                .bool("available", "false = ran out, true = back in stock", true)
                                .build(),
                        Set.of(OWNER, COOK), false,
                        (a, member) -> {
                            Ingredient i = ingredients.setAvailability(a.requiredId("ingredient_id"), a.requiredBool("available"));
                            return Map.of("id", i.id(), "name", i.name(), "available", i.available());
                        }),
                new McpTool("list_deliveries", "Domicilios",
                        "Delivery orders not charged yet, oldest first: status (PENDING = waiting for a waiter to confirm, "
                                + "CONFIRMED, PREPARING, READY, ON_THE_WAY), neighbourhood, distance, payment method and items.",
                        Schemas.object().build(),
                        Set.of(OWNER, WAITER), true,
                        (a, member) -> deliveries.open().stream().map(RestaurantTools::delivery).toList()));
    }

    private static ReportPeriod period(String value) {
        if (value == null) return ReportPeriod.TODAY;
        try {
            return ReportPeriod.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("period must be TODAY, WEEK or MONTH");
        }
    }

    private Map<String, Object> tableDetail(long number) {
        TableSummary found = floor.floor().stream()
                .filter(t -> t.table().number() == number)
                .findFirst()
                .orElseThrow(TableNotFoundException::new);
        TableDetail detail = floor.table(found.table().id());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("number", detail.table().number());
        m.put("open", detail.session() != null);
        m.put("openedAt", detail.session() != null ? detail.session().openedAt() : null);
        m.put("total", detail.total());
        m.put("rounds", detail.tickets().stream().map(RestaurantTools::ticket).toList());
        return m;
    }

    private static Map<String, Object> table(TableSummary t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("number", t.table().number());
        m.put("label", t.table().label());
        m.put("active", t.table().active());
        m.put("open", t.session() != null);
        m.put("openedAt", t.session() != null ? t.session().openedAt() : null);
        m.put("total", t.total());
        m.put("waitingApproval", t.pendingApproval());
        m.put("inKitchen", t.inKitchen());
        m.put("ready", t.ready());
        return m;
    }

    private static Map<String, Object> ticket(OrderTicket t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.id());
        if (t.isDelivery()) m.put("delivery", t.deliveryId());
        else m.put("table", t.tableNumber());
        m.put("status", t.status());
        m.put("source", t.source());
        m.put("total", t.total());
        m.put("createdAt", t.createdAt());
        m.put("readyAt", t.readyAt());
        m.put("deliveredAt", t.deliveredAt());
        if (t.cancelReason() != null) m.put("cancelReason", t.cancelReason());
        m.put("items", t.items().stream().map(RestaurantTools::item).toList());
        return m;
    }

    private static Map<String, Object> item(TicketItem i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("qty", i.qty());
        m.put("name", i.name());
        m.put("unitPrice", i.unitPrice());
        if (!i.modifiers().isEmpty()) m.put("options", i.modifiers().stream().map(TicketItemModifier::name).toList());
        if (!i.removed().isEmpty()) m.put("without", i.removed());
        if (i.note() != null) m.put("note", i.note());
        return m;
    }

    private static List<Map<String, Object>> menu(MenuView view) {
        return view.categories().stream().map(c -> category(c, view.dishes())).toList();
    }

    private static Map<String, Object> category(Category c, List<Dish> all) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("category", c.name());
        m.put("dishes", all.stream().filter(d -> d.categoryId().equals(c.id())).map(RestaurantTools::dish).toList());
        return m;
    }

    private static Map<String, Object> dish(Dish d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.id());
        m.put("name", d.name());
        m.put("price", d.price());
        m.put("available", d.orderable());
        if (!d.available()) m.put("markedSoldOut", true);
        else if (d.soldOutReason() != null) m.put("ranOut", d.soldOutReason());
        if (d.description() != null) m.put("description", d.description());
        m.put("ingredients", d.ingredients().stream().map(RestaurantTools::ingredient).toList());
        if (!d.leftOut().isEmpty()) m.put("servedWithout", d.leftOut().stream().map(DishIngredient::name).toList());
        List<String> removable = d.ingredients().stream().filter(DishIngredient::removable).map(DishIngredient::name).toList();
        if (!removable.isEmpty()) m.put("canRemove", removable);
        if (!d.modifiers().isEmpty()) m.put("options", d.modifiers().stream().map(RestaurantTools::group).toList());
        return m;
    }

    private static Map<String, Object> ingredient(DishIngredient i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", i.ingredientId());
        m.put("name", i.name());
        if (!i.available()) m.put("ranOut", true);
        return m;
    }

    private static Map<String, Object> group(ModifierGroup g) {
        return Map.of("name", g.name(), "min", g.min(), "max", g.max(),
                "choices", g.options().stream().map(RestaurantTools::choice).toList());
    }

    private static String choice(ModifierOption o) {
        String text = o.price() == 0 ? o.name() : o.name() + " (+" + o.price() + ")";
        return o.available() ? text : text + " [agotado]";
    }

    // No phone or exact address: the assistant does not need them to answer about the floor
    private static Map<String, Object> delivery(DeliveryView v) {
        DeliveryOrder d = v.delivery();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.id());
        m.put("status", DeliveryStatus.of(v));
        m.put("customer", d.customerName());
        m.put("area", d.placeLabel());
        m.put("distanceMeters", d.distanceMeters());
        m.put("payment", d.paymentMethodName());
        m.put("total", v.total());
        m.put("createdAt", d.createdAt());
        m.put("items", v.tickets().stream().flatMap(t -> t.items().stream()).map(RestaurantTools::item).toList());
        return m;
    }
}
