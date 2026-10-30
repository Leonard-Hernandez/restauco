package com.lsoftware.restauco.entity.voice.model;

import java.time.OffsetDateTime;

/**
 * A dictated order kept for later: what was said, what the parser understood, and the round the
 * waiter finally sent (null until then). Pairs of "said" and "sent" are the examples a model
 * would be trained on.
 */
public record VoiceSample(
        Long id,
        String transcript,
        VoiceOrder parsed,
        Long ticketId,
        String createdBy,
        OffsetDateTime createdAt
) {
    public VoiceSample withTicket(Long ticket) {
        return new VoiceSample(id, transcript, parsed, ticket, createdBy, createdAt);
    }
}
