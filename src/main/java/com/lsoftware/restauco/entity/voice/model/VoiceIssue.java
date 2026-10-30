package com.lsoftware.restauco.entity.voice.model;

/**
 * Something said that could not become a line. code: NEEDS_CHOICES (a dish whose options were
 * missing or did not fit; dishId says which, so the waiter can pick them), UNAVAILABLE (sold out)
 * or NOT_UNDERSTOOD (no dish found). heard: the words it came from.
 */
public record VoiceIssue(
        String code,
        Long dishId,
        String dishName,
        String heard
) {}
