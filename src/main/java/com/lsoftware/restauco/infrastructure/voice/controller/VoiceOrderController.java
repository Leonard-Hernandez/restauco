package com.lsoftware.restauco.infrastructure.voice.controller;

import com.lsoftware.restauco.infrastructure.voice.dto.VoiceOrderRequest;
import com.lsoftware.restauco.infrastructure.voice.dto.VoiceOrderResponse;
import com.lsoftware.restauco.infrastructure.voice.dto.VoiceSentRequest;
import com.lsoftware.restauco.usecase.voice.TakeVoiceOrderUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/panel/floor/voice-orders")
@RequiredArgsConstructor
@Tag(name = "Floor (waiter)", description = "Tables, orders and bills")
@SecurityRequirement(name = "bearerAuth")
public class VoiceOrderController {

    private final TakeVoiceOrderUseCase voice;

    @PostMapping
    @Operation(summary = "Understand a dictated order: lines for the waiter to review, never sent on its own")
    public VoiceOrderResponse understand(@Valid @RequestBody VoiceOrderRequest req, @AuthenticationPrincipal Jwt jwt) {
        return VoiceOrderResponse.from(voice.understand(req.text(), jwt.getSubject()));
    }

    @PostMapping("/{id}/sent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Link a dictated order to the round finally sent (training examples)")
    public void sent(@PathVariable Long id, @Valid @RequestBody VoiceSentRequest req) {
        voice.sent(id, req.ticketId());
    }
}
