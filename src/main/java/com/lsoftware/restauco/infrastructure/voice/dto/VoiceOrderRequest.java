package com.lsoftware.restauco.infrastructure.voice.dto;

import com.lsoftware.restauco.usecase.voice.TakeVoiceOrderUseCase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** What the phone's speech-to-text wrote (or the waiter typed). */
public record VoiceOrderRequest(@NotBlank @Size(max = TakeVoiceOrderUseCase.MAX_TEXT) String text) {}
