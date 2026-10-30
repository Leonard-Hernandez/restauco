package com.lsoftware.restauco.entity.floor.model;

import java.util.UUID;

public record DiningTable(
        Long id,
        int number,
        /** Optional display name ("Terraza 2"); the number is used otherwise. */
        String label,
        /** Printed in the table QR; anyone holding it can order for this table. */
        UUID qrToken,
        boolean active
) {
    public String displayName() {
        return label != null && !label.isBlank() ? label : "Mesa " + number;
    }

    public DiningTable withQrToken(UUID token) {
        return new DiningTable(id, number, label, token, active);
    }
}
