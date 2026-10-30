package com.lsoftware.restauco.infrastructure.floor.dto;

import com.lsoftware.restauco.usecase.floor.dto.TableSummary;

import java.time.OffsetDateTime;

public record TableSummaryResponse(
        Long id,
        int number,
        String name,
        boolean active,
        Long sessionId,
        OffsetDateTime openedAt,
        int pendingApproval,
        int inKitchen,
        int ready,
        long total
) {
    public static TableSummaryResponse from(TableSummary s) {
        return new TableSummaryResponse(s.table().id(), s.table().number(), s.table().displayName(), s.table().active(),
                s.session() != null ? s.session().id() : null, s.session() != null ? s.session().openedAt() : null,
                s.pendingApproval(), s.inKitchen(), s.ready(), s.total());
    }
}
