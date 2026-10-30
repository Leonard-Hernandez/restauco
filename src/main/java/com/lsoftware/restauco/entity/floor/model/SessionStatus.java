package com.lsoftware.restauco.entity.floor.model;

public enum SessionStatus {
    OPEN,
    CLOSED,
    /** A delivery rejected before anything was charged. */
    CANCELLED
}
