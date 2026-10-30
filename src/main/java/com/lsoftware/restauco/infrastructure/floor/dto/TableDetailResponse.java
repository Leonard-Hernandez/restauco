package com.lsoftware.restauco.infrastructure.floor.dto;

import com.lsoftware.restauco.infrastructure.order.dto.TicketResponse;
import com.lsoftware.restauco.usecase.floor.dto.TableDetail;

import java.util.List;

public record TableDetailResponse(
        TableResponse table,
        SessionResponse session,
        List<TicketResponse> tickets,
        long total
) {
    public static TableDetailResponse from(TableDetail d) {
        return new TableDetailResponse(TableResponse.from(d.table()), SessionResponse.from(d.session()),
                d.tickets().stream().map(TicketResponse::from).toList(), d.total());
    }
}
