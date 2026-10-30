package com.lsoftware.restauco.infrastructure.report.dto;

import com.lsoftware.restauco.entity.report.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SalesReportResponse(
        ReportPeriod period,
        ReportPeriod.Granularity granularity,
        LocalDate from,
        LocalDate to,
        Totals current,
        Totals previous,
        List<Bucket> timeline,
        List<Breakdown> byPaymentMethod,
        List<Breakdown> byWaiter,
        List<DishSales> topDishes,
        KitchenTotals kitchen
) {

    public record Totals(long sales, int bills, long averageTicket) {
        static Totals from(SalesTotals t) {
            return new Totals(t.sales(), t.bills(), t.averageTicket());
        }
    }

    /** {@code start} is the restaurant's local time, without offset. */
    public record Bucket(LocalDateTime start, long sales, int bills) {}

    public static SalesReportResponse from(SalesReport r) {
        return new SalesReportResponse(
                r.period(), r.period().granularity(), r.from(), r.to(),
                Totals.from(r.current()), Totals.from(r.previous()),
                r.timeline().stream().map(b -> new Bucket(b.start(), b.sales(), b.bills())).toList(),
                r.byPaymentMethod(), r.byWaiter(), r.topDishes(), r.kitchen());
    }
}
