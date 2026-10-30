package com.lsoftware.restauco.entity.report.model;

import java.time.LocalDateTime;

/** Sales of one hour or day, starting at {@code start} (restaurant's local time). */
public record TimeBucket(LocalDateTime start, long sales, int bills) {}
