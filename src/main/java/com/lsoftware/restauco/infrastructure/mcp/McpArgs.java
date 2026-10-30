package com.lsoftware.restauco.infrastructure.mcp;

import com.lsoftware.restauco.entity.exception.DomainException;

import java.util.Map;

/** Typed access to the arguments of a tool call. */
public record McpArgs(Map<String, Object> values) {

    public String string(String name) {
        Object v = values.get(name);
        return v == null || v.toString().isBlank() ? null : v.toString().strip();
    }

    public String requiredString(String name) {
        String v = string(name);
        if (v == null) throw missing(name);
        return v;
    }

    public Long id(String name) {
        Object v = values.get(name);
        if (v == null || v.toString().isBlank()) return null;
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(v.toString().strip());
        } catch (NumberFormatException e) {
            throw new DomainException("INVALID_ARGUMENT", name + " must be a number");
        }
    }

    public Long requiredId(String name) {
        Long v = id(name);
        if (v == null) throw missing(name);
        return v;
    }

    public Boolean requiredBool(String name) {
        Object v = values.get(name);
        if (v instanceof Boolean b) return b;
        if (v != null && ("true".equalsIgnoreCase(v.toString()) || "false".equalsIgnoreCase(v.toString()))) {
            return Boolean.parseBoolean(v.toString());
        }
        throw v == null ? missing(name) : new DomainException("INVALID_ARGUMENT", name + " must be true or false");
    }

    private static DomainException missing(String name) {
        return new DomainException("INVALID_ARGUMENT", "Missing argument: " + name);
    }
}
