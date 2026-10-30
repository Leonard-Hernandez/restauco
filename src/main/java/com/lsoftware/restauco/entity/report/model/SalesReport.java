package com.lsoftware.restauco.entity.report.model;

import java.time.LocalDate;
import java.util.List;

/**
 * The owner's numbers for a period. {@code previous} is the period just before, cut at the
 * same time of day, so "today so far" compares with "yesterday up to this hour".
 */
public record SalesReport(
        ReportPeriod period,
        LocalDate from,
        LocalDate to,
        SalesTotals current,
        SalesTotals previous,
        List<TimeBucket> timeline,
        List<Breakdown> byPaymentMethod,
        List<Breakdown> byWaiter,
        List<DishSales> topDishes,
        KitchenTotals kitchen
) {}
