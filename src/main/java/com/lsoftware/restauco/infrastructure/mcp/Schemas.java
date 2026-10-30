package com.lsoftware.restauco.infrastructure.mcp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small builder for the JSON Schemas of the tool inputs. */
final class Schemas {

    private final Map<String, Object> properties = new LinkedHashMap<>();
    private final List<String> required = new java.util.ArrayList<>();

    static Schemas object() {
        return new Schemas();
    }

    Schemas string(String name, String description, boolean mandatory) {
        return prop(name, Map.of("type", "string", "description", description), mandatory);
    }

    Schemas integer(String name, String description, boolean mandatory) {
        return prop(name, Map.of("type", "integer", "description", description), mandatory);
    }

    Schemas bool(String name, String description, boolean mandatory) {
        return prop(name, Map.of("type", "boolean", "description", description), mandatory);
    }

    Schemas enumeration(String name, String description, List<String> values, boolean mandatory) {
        return prop(name, Map.of("type", "string", "enum", values, "description", description), mandatory);
    }

    Map<String, Object> build() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (!required.isEmpty()) schema.put("required", required);
        schema.put("additionalProperties", false);
        return schema;
    }

    private Schemas prop(String name, Map<String, Object> definition, boolean mandatory) {
        properties.put(name, definition);
        if (mandatory) required.add(name);
        return this;
    }
}
