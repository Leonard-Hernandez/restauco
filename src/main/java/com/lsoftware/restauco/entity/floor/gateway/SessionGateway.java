package com.lsoftware.restauco.entity.floor.gateway;

import com.lsoftware.restauco.entity.floor.model.TableSession;

import java.util.List;
import java.util.Optional;

public interface SessionGateway {
    TableSession save(TableSession session);
    Optional<TableSession> findById(Long id);
    Optional<TableSession> findOpenByTable(Long tableId);
    /** Open bills of tables (deliveries have their own list). */
    List<TableSession> findAllOpen();
}
