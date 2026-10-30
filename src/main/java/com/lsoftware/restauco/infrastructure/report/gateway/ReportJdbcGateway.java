package com.lsoftware.restauco.infrastructure.report.gateway;

import com.lsoftware.restauco.entity.report.gateway.ReportGateway;
import com.lsoftware.restauco.entity.report.model.*;
import com.lsoftware.restauco.entity.tenant.gateway.CurrentTenantGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Plain SQL aggregates: Hibernate's @TenantId filter does not reach native queries, so every
 * query filters tenant_id itself. Sales are bills closed in the range; dishes and kitchen
 * times are rounds ordered in it (only rounds that reached the kitchen count as sold).
 */
@Component
@RequiredArgsConstructor
public class ReportJdbcGateway implements ReportGateway {

    private static final String CLOSED = """
            FROM table_session s
            WHERE s.tenant_id = :tenant AND s.status = 'CLOSED'
              AND s.closed_at >= :from AND s.closed_at < :to
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final CurrentTenantGateway tenants;

    @Override
    public SalesTotals totals(Instant from, Instant to) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(s.total), 0) AS sales, COUNT(*) AS bills " + CLOSED,
                params(from, to), (rs, i) -> new SalesTotals(rs.getLong("sales"), rs.getInt("bills")));
    }

    @Override
    public List<TimeBucket> timeline(Instant from, Instant to, ZoneId zone, ReportPeriod.Granularity granularity) {
        String sql = """
                SELECT date_trunc(:unit, s.closed_at AT TIME ZONE :zone) AS bucket,
                       SUM(s.total) AS sales, COUNT(*) AS bills
                """ + CLOSED + " GROUP BY bucket ORDER BY bucket";
        MapSqlParameterSource p = params(from, to)
                .addValue("unit", granularity == ReportPeriod.Granularity.HOUR ? "hour" : "day")
                .addValue("zone", zone.getId());
        return jdbc.query(sql, p, (rs, i) -> new TimeBucket(
                rs.getTimestamp("bucket").toLocalDateTime(), rs.getLong("sales"), rs.getInt("bills")));
    }

    @Override
    public List<Breakdown> byPaymentMethod(Instant from, Instant to) {
        // A split bill counts once for each method, with only the part paid with it
        String sql = """
                SELECT COALESCE(p.part ->> 'name', 'Sin método') AS label,
                       COUNT(*) AS bills, SUM((p.part ->> 'amount')::bigint) AS sales
                FROM table_session s
                CROSS JOIN LATERAL jsonb_array_elements(s.payments) AS p(part)
                WHERE s.tenant_id = :tenant AND s.status = 'CLOSED'
                  AND s.closed_at >= :from AND s.closed_at < :to
                GROUP BY label ORDER BY sales DESC, label
                """;
        return jdbc.query(sql, params(from, to), ReportJdbcGateway::breakdown);
    }

    @Override
    public List<Breakdown> byWaiter(Instant from, Instant to) {
        String sql = """
                SELECT COALESCE(m.name, m.email, 'Sin asignar') AS label,
                       COUNT(*) AS bills, SUM(s.total) AS sales
                FROM table_session s
                LEFT JOIN tenant_member m ON m.tenant_id = s.tenant_id AND m.authco_sub = s.closed_by
                WHERE s.tenant_id = :tenant AND s.status = 'CLOSED'
                  AND s.closed_at >= :from AND s.closed_at < :to
                GROUP BY label ORDER BY sales DESC, label
                """;
        return jdbc.query(sql, params(from, to), ReportJdbcGateway::breakdown);
    }

    @Override
    public List<DishSales> topDishes(Instant from, Instant to, int limit) {
        // The name of the latest round, in case the dish was renamed meanwhile
        String sql = """
                SELECT (i ->> 'dishId')::bigint AS dish_id,
                       (array_agg(i ->> 'name' ORDER BY t.created_at DESC))[1] AS name,
                       SUM((i ->> 'qty')::int) AS units,
                       SUM((i ->> 'qty')::bigint * (i ->> 'unitPrice')::bigint) AS sales
                FROM order_ticket t, jsonb_array_elements(t.items) AS i
                WHERE t.tenant_id = :tenant AND t.status IN ('SENT', 'PREPARING', 'READY', 'DELIVERED')
                  AND t.created_at >= :from AND t.created_at < :to
                GROUP BY dish_id
                ORDER BY units DESC, sales DESC, name
                LIMIT :limit
                """;
        return jdbc.query(sql, params(from, to).addValue("limit", limit), (rs, i) -> new DishSales(
                rs.getLong("dish_id"), rs.getString("name"), rs.getInt("units"), rs.getLong("sales")));
    }

    @Override
    public KitchenTotals kitchen(Instant from, Instant to) {
        String sql = """
                SELECT COUNT(*) FILTER (WHERE t.sent_at IS NOT NULL AND t.status <> 'CANCELLED') AS rounds,
                       COUNT(*) FILTER (WHERE t.sent_at IS NOT NULL AND t.status <> 'CANCELLED' AND t.source = 'QR') AS from_qr,
                       COUNT(*) FILTER (WHERE t.status = 'CANCELLED') AS cancelled,
                       AVG(EXTRACT(EPOCH FROM t.ready_at - t.sent_at)) / 60 AS prep,
                       AVG(EXTRACT(EPOCH FROM t.delivered_at - t.ready_at)) / 60 AS pickup
                FROM order_ticket t
                WHERE t.tenant_id = :tenant AND t.created_at >= :from AND t.created_at < :to
                """;
        return jdbc.queryForObject(sql, params(from, to), (rs, i) -> new KitchenTotals(
                rs.getInt("rounds"), rs.getInt("from_qr"), rs.getInt("cancelled"),
                nullableDouble(rs, "prep"), nullableDouble(rs, "pickup")));
    }

    private MapSqlParameterSource params(Instant from, Instant to) {
        return new MapSqlParameterSource()
                .addValue("tenant", tenants.current().id())
                .addValue("from", OffsetDateTime.ofInstant(from, ZoneOffset.UTC))
                .addValue("to", OffsetDateTime.ofInstant(to, ZoneOffset.UTC));
    }

    private static Breakdown breakdown(ResultSet rs, int row) throws SQLException {
        return new Breakdown(rs.getString("label"), rs.getInt("bills"), rs.getLong("sales"));
    }

    private static Double nullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }
}
