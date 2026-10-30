package com.lsoftware.restauco.infrastructure.voice.dto;

import jakarta.validation.constraints.NotNull;

public record VoiceSentRequest(@NotNull Long ticketId) {}
