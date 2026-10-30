package com.lsoftware.restauco.infrastructure.voice.dto;

import com.lsoftware.restauco.entity.voice.model.VoiceIssue;
import com.lsoftware.restauco.entity.voice.model.VoiceLine;
import com.lsoftware.restauco.usecase.voice.TakeVoiceOrderUseCase;

import java.util.List;

public record VoiceOrderResponse(
        Long sampleId,
        String transcript,
        Integer tableNumber,
        List<VoiceLine> lines,
        List<VoiceIssue> issues
) {
    public static VoiceOrderResponse from(TakeVoiceOrderUseCase.Understood u) {
        return new VoiceOrderResponse(u.sampleId(), u.order().transcript(), u.order().tableNumber(),
                u.order().lines(), u.order().issues());
    }
}
