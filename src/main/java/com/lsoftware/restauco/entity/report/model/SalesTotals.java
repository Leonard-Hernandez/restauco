package com.lsoftware.restauco.entity.report.model;

/** Closed bills of a period. */
public record SalesTotals(long sales, int bills) {

    public static final SalesTotals NONE = new SalesTotals(0, 0);

    public long averageTicket() {
        return bills == 0 ? 0 : Math.round((double) sales / bills);
    }
}
