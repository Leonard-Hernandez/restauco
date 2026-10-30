package com.lsoftware.restauco.entity.report.model;

/** Closed bills grouped by something (a payment method, the waiter who charged them). */
public record Breakdown(String label, int bills, long sales) {}
