package com.lsoftware.restauco.entity.report.gateway;

import com.lsoftware.restauco.entity.report.model.*;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

/** Aggregates over closed bills and rounds of the current restaurant, in [from, to). */
public interface ReportGateway {

    SalesTotals totals(Instant from, Instant to);

    /** Only buckets with sales, oldest first. */
    List<TimeBucket> timeline(Instant from, Instant to, ZoneId zone, ReportPeriod.Granularity granularity);

    List<Breakdown> byPaymentMethod(Instant from, Instant to);

    List<Breakdown> byWaiter(Instant from, Instant to);

    List<DishSales> topDishes(Instant from, Instant to, int limit);

    KitchenTotals kitchen(Instant from, Instant to);
}
