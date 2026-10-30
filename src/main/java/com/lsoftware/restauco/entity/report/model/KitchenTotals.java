package com.lsoftware.restauco.entity.report.model;

/**
 * Rounds ordered in a period. Minutes are averages, null without data:
 * prep = sent to cooked, pickup = cooked to on the table.
 */
public record KitchenTotals(int rounds, int fromQr, int cancelled, Double prepMinutes, Double pickupMinutes) {}
