package com.lsoftware.restauco.infrastructure.floor.dto;

import com.lsoftware.restauco.entity.floor.model.DiningTable;

import java.util.UUID;

public record TableResponse(Long id, int number, String label, String name, UUID qrToken, boolean active) {
    public static TableResponse from(DiningTable t) {
        return new TableResponse(t.id(), t.number(), t.label(), t.displayName(), t.qrToken(), t.active());
    }
}
