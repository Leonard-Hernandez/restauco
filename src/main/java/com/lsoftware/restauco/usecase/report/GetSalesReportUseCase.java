package com.lsoftware.restauco.usecase.report;

import com.lsoftware.restauco.entity.report.gateway.ReportGateway;
import com.lsoftware.restauco.entity.report.model.ReportPeriod;
import com.lsoftware.restauco.entity.report.model.SalesReport;
import com.lsoftware.restauco.entity.report.model.TimeBucket;
import com.lsoftware.restauco.entity.tenant.gateway.CurrentTenantGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Sales, best sellers and kitchen times for the owner, counted in the restaurant's timezone. */
@Component
@RequiredArgsConstructor
public class GetSalesReportUseCase {

    static final int TOP_DISHES = 8;

    private final ReportGateway reports;
    private final CurrentTenantGateway tenants;
    private final Clock clock;

    @Transactional(readOnly = true)
    public SalesReport execute(ReportPeriod period) {
        ZoneId zone = tenants.current().timezone();
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(zone);
        LocalDate today = now.toLocalDate();
        LocalDate first = today.minusDays(period.days() - 1L);

        Instant from = first.atStartOfDay(zone).toInstant();
        Instant to = now.toInstant();
        Instant previousFrom = first.minusDays(period.days()).atStartOfDay(zone).toInstant();
        Instant previousTo = now.minusDays(period.days()).toInstant();

        List<TimeBucket> sparse = reports.timeline(from, to, zone, period.granularity());
        return new SalesReport(
                period, first, today,
                reports.totals(from, to),
                reports.totals(previousFrom, previousTo),
                fill(sparse, period, first, now.toLocalDateTime()),
                reports.byPaymentMethod(from, to),
                reports.byWaiter(from, to),
                reports.topDishes(from, to, TOP_DISHES),
                reports.kitchen(from, to));
    }

    /**
     * Zero buckets between the sales, so the chart keeps its time scale: every day of the
     * period, or for today the hours from the first sale up to now (the restaurant only
     * opens in the evening; a 24-hour axis would be mostly empty).
     */
    static List<TimeBucket> fill(List<TimeBucket> sparse, ReportPeriod period, LocalDate first, LocalDateTime now) {
        Map<LocalDateTime, TimeBucket> byStart = sparse.stream()
                .collect(Collectors.toMap(TimeBucket::start, Function.identity(), (a, b) -> a));
        LocalDateTime start;
        LocalDateTime end;
        ChronoUnit step;
        if (period.granularity() == ReportPeriod.Granularity.HOUR) {
            if (sparse.isEmpty()) return List.of();
            step = ChronoUnit.HOURS;
            start = sparse.getFirst().start();
            end = now.truncatedTo(ChronoUnit.HOURS);
        } else {
            step = ChronoUnit.DAYS;
            start = first.atStartOfDay();
            end = now.toLocalDate().atStartOfDay();
        }
        List<TimeBucket> filled = new ArrayList<>();
        for (LocalDateTime t = start; !t.isAfter(end); t = t.plus(1, step)) {
            filled.add(byStart.getOrDefault(t, new TimeBucket(t, 0, 0)));
        }
        return filled;
    }
}
