package com.lsoftware.restauco.usecase.report;

import com.lsoftware.restauco.entity.report.gateway.ReportGateway;
import com.lsoftware.restauco.entity.report.model.*;
import com.lsoftware.restauco.entity.tenant.model.Tenant;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GetSalesReportUseCaseTest {

    static final ZoneId CALI = ZoneId.of("America/Bogota");
    // 20 Oct 2026, 9:30 p. m. in Cali (UTC-5)
    static final Instant NOW = Instant.parse("2026-10-21T02:30:00Z");

    /** Records the ranges asked for and answers with canned timelines. */
    static class FakeReports implements ReportGateway {
        final List<Instant[]> totalsRanges = new ArrayList<>();
        List<TimeBucket> timeline = List.of();

        @Override
        public SalesTotals totals(Instant from, Instant to) {
            totalsRanges.add(new Instant[]{from, to});
            return new SalesTotals(totalsRanges.size() == 1 ? 100_000 : 50_000, 4);
        }

        @Override
        public List<TimeBucket> timeline(Instant from, Instant to, ZoneId zone, ReportPeriod.Granularity g) {
            return timeline;
        }

        @Override
        public List<Breakdown> byPaymentMethod(Instant from, Instant to) {
            return List.of();
        }

        @Override
        public List<Breakdown> byWaiter(Instant from, Instant to) {
            return List.of();
        }

        @Override
        public List<DishSales> topDishes(Instant from, Instant to, int limit) {
            return List.of();
        }

        @Override
        public KitchenTotals kitchen(Instant from, Instant to) {
            return new KitchenTotals(0, 0, 0, null, null);
        }
    }

    final FakeReports reports = new FakeReports();
    final GetSalesReportUseCase useCase = new GetSalesReportUseCase(reports,
            () -> new Tenant(1L, "toches-arepas", "Toche's", CALI, null, null, Map.of()),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void todayStartsAtLocalMidnightAndComparesWithYesterdayUpToThisHour() {
        SalesReport report = useCase.execute(ReportPeriod.TODAY);

        assertThat(report.from()).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(reports.totalsRanges.get(0))
                .containsExactly(Instant.parse("2026-10-20T05:00:00Z"), NOW);
        assertThat(reports.totalsRanges.get(1))
                .containsExactly(Instant.parse("2026-10-19T05:00:00Z"), Instant.parse("2026-10-20T02:30:00Z"));
        assertThat(report.current().sales()).isEqualTo(100_000);
        assertThat(report.current().averageTicket()).isEqualTo(25_000);
    }

    @Test
    void weekCoversSevenDaysWithTodayAndFillsEveryDay() {
        reports.timeline = List.of(new TimeBucket(LocalDateTime.of(2026, 10, 16, 0, 0), 30_000, 2));

        SalesReport report = useCase.execute(ReportPeriod.WEEK);

        assertThat(report.from()).isEqualTo(LocalDate.of(2026, 10, 14));
        assertThat(reports.totalsRanges.get(1)[0]).isEqualTo(Instant.parse("2026-10-07T05:00:00Z"));
        assertThat(report.timeline()).hasSize(7);
        assertThat(report.timeline().get(2).sales()).isEqualTo(30_000);
        assertThat(report.timeline()).filteredOn(b -> b.sales() == 0).hasSize(6);
    }

    @Test
    void todayTimelineRunsFromTheFirstSaleToNow() {
        reports.timeline = List.of(new TimeBucket(LocalDateTime.of(2026, 10, 20, 18, 0), 20_000, 1),
                new TimeBucket(LocalDateTime.of(2026, 10, 20, 20, 0), 45_000, 2));

        List<TimeBucket> timeline = useCase.execute(ReportPeriod.TODAY).timeline();

        assertThat(timeline).extracting(b -> b.start().getHour()).containsExactly(18, 19, 20, 21);
        assertThat(timeline).extracting(TimeBucket::sales).containsExactly(20_000L, 0L, 45_000L, 0L);
    }

    @Test
    void quietDayHasNoTimeline() {
        assertThat(useCase.execute(ReportPeriod.TODAY).timeline()).isEmpty();
    }
}
