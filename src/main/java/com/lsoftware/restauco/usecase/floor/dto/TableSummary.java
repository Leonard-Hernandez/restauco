package com.lsoftware.restauco.usecase.floor.dto;

import com.lsoftware.restauco.entity.floor.model.DiningTable;
import com.lsoftware.restauco.entity.floor.model.TableSession;

/** A table on the waiter's floor map. session is null while the table is free. */
public record TableSummary(
        DiningTable table,
        TableSession session,
        int pendingApproval,
        int inKitchen,
        int ready,
        long total
) {}
