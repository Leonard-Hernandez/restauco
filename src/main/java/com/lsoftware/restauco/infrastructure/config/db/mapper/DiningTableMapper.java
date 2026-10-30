package com.lsoftware.restauco.infrastructure.config.db.mapper;

import com.lsoftware.restauco.entity.floor.model.DiningTable;
import com.lsoftware.restauco.infrastructure.config.db.schema.DiningTableEntity;
import org.springframework.stereotype.Component;

@Component
public class DiningTableMapper {

    public DiningTable toDomain(DiningTableEntity e) {
        return new DiningTable(e.getId(), e.getNumber(), e.getLabel(), e.getQrToken(), e.isActive());
    }

    public DiningTableEntity toEntity(DiningTable t) {
        return DiningTableEntity.builder()
                .id(t.id())
                .number(t.number())
                .label(t.label())
                .qrToken(t.qrToken())
                .active(t.active())
                .build();
    }
}
