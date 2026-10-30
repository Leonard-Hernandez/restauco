package com.lsoftware.restauco.entity.voice.gateway;

import com.lsoftware.restauco.entity.voice.model.VoiceSample;

import java.util.Optional;

public interface VoiceSampleGateway {
    VoiceSample save(VoiceSample sample);
    Optional<VoiceSample> findById(Long id);
}
