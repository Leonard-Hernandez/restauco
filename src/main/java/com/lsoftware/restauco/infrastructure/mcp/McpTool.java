package com.lsoftware.restauco.infrastructure.mcp;

import com.lsoftware.restauco.entity.member.model.MemberRole;
import com.lsoftware.restauco.entity.member.model.TenantMember;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * One tool the assistant can call. inputSchema is a JSON Schema object; handler receives the call
 * arguments and the member asking, and returns any value, sent back as JSON. roles: who may see
 * and call it (the same split as the panel: sales for the owner, the floor for waiters...).
 */
public record McpTool(String name, String title, String description, Map<String, Object> inputSchema,
                      Set<MemberRole> roles, boolean readOnly,
                      BiFunction<McpArgs, TenantMember, Object> handler) {

    public boolean allows(TenantMember member) {
        return roles.contains(member.role());
    }

    public Map<String, Object> descriptor() {
        return Map.of(
                "name", name,
                "title", title,
                "description", description,
                "inputSchema", inputSchema,
                "annotations", Map.of("readOnlyHint", readOnly, "destructiveHint", false,
                        "idempotentHint", true, "openWorldHint", false));
    }
}
