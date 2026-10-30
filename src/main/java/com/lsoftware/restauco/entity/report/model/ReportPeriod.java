package com.lsoftware.restauco.entity.report.model;

/** Days counted back from today (today included), in the restaurant's timezone. */
public enum ReportPeriod {
    TODAY(1), WEEK(7), MONTH(30);

    private final int days;

    ReportPeriod(int days) {
        this.days = days;
    }

    public int days() {
        return days;
    }

    /** One day is read by hour, longer periods by day. */
    public Granularity granularity() {
        return this == TODAY ? Granularity.HOUR : Granularity.DAY;
    }

    public enum Granularity { HOUR, DAY }
}
