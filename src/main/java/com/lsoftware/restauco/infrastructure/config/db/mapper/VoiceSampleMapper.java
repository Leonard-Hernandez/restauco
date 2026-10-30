package com.lsoftware.restauco.infrastructure.config.db.mapper;

import com.lsoftware.restauco.entity.voice.model.VoiceSample;
import com.lsoftware.restauco.infrastructure.config.db.schema.VoiceSampleEntity;
import org.springframework.stereotype.Component;

@Component
public class VoiceSampleMapper {

    public VoiceSample toDomain(VoiceSampleEntity e) {
        return new VoiceSample(e.getId(), e.getTranscript(), e.getParsed(), e.getTicketId(),
                e.getCreatedBy(), e.getCreatedAt());
    }

    public VoiceSampleEntity toEntity(VoiceSample s) {
        return VoiceSampleEntity.builder()
                .id(s.id())
                .transcript(s.transcript())
                .parsed(s.parsed())
                .ticketId(s.ticketId())
                .createdBy(s.createdBy())
                .createdAt(s.createdAt())
                .build();
    }
}
