package com.lsoftware.restauco.usecase.floor;

import com.lsoftware.restauco.entity.floor.exception.TableNotFoundException;
import com.lsoftware.restauco.entity.floor.exception.TableNumberTakenException;
import com.lsoftware.restauco.entity.floor.gateway.TableGateway;
import com.lsoftware.restauco.entity.floor.model.DiningTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ManageTableUseCase {

    private final TableGateway gateway;

    @Transactional(readOnly = true)
    public List<DiningTable> list() {
        return gateway.findAll();
    }

    @Transactional
    public DiningTable create(int number, String label, boolean active) {
        if (gateway.existsByNumber(number, null)) throw new TableNumberTakenException();
        return gateway.save(new DiningTable(null, number, clean(label), UUID.randomUUID(), active));
    }

    @Transactional
    public DiningTable update(Long id, int number, String label, boolean active) {
        DiningTable existing = gateway.findById(id).orElseThrow(TableNotFoundException::new);
        if (gateway.existsByNumber(number, id)) throw new TableNumberTakenException();
        return gateway.save(new DiningTable(id, number, clean(label), existing.qrToken(), active));
    }

    /** New QR for the table: printed copies of the old one stop working. */
    @Transactional
    public DiningTable regenerateQr(Long id) {
        DiningTable existing = gateway.findById(id).orElseThrow(TableNotFoundException::new);
        return gateway.save(existing.withQrToken(UUID.randomUUID()));
    }

    private static String clean(String label) {
        return label == null || label.isBlank() ? null : label.strip();
    }
}
