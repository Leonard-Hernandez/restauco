package com.lsoftware.restauco.entity.voice.model;

import java.util.List;

/** What a dictated order said: never sent on its own, the waiter reviews it first. */
public record VoiceOrder(
        String transcript,
        /** "mesa 3" -> 3, or null when no table was said */
        Integer tableNumber,
        List<VoiceLine> lines,
        List<VoiceIssue> issues
) {
    public VoiceOrder {
        lines = List.copyOf(lines);
        issues = List.copyOf(issues);
    }
}
