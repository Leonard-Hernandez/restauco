package com.lsoftware.restauco.entity.voice.exception;

import com.lsoftware.restauco.entity.exception.NotFoundException;

public class VoiceSampleNotFoundException extends NotFoundException {
    public VoiceSampleNotFoundException() {
        super("VOICE_SAMPLE_NOT_FOUND", "Dictated order not found");
    }
}
