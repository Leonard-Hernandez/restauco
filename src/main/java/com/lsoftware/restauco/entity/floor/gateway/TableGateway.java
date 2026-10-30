package com.lsoftware.restauco.entity.floor.gateway;

import com.lsoftware.restauco.entity.floor.model.DiningTable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TableGateway {
    DiningTable save(DiningTable table);
    Optional<DiningTable> findById(Long id);
    /** Locks the table row until the transaction ends (serializes opening its bill). */
    Optional<DiningTable> lockById(Long id);
    Optional<DiningTable> findByQrToken(UUID token);
    List<DiningTable> findAll();
    boolean existsByNumber(int number, Long exceptId);
}
