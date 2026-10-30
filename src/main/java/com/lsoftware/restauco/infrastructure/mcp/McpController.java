package com.lsoftware.restauco.infrastructure.mcp;

import com.lsoftware.restauco.entity.exception.DomainException;
import com.lsoftware.restauco.entity.member.model.TenantMember;
import com.lsoftware.restauco.entity.tenant.gateway.TenantGateway;
import com.lsoftware.restauco.entity.tenant.model.Tenant;
import com.lsoftware.restauco.infrastructure.config.tenant.TenantContext;
import com.lsoftware.restauco.usecase.member.ResolveMemberUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * MCP server (Streamable HTTP, stateless, JSON responses) for the restaurant in the path, e.g.
 * https://host/restauco-api/mcp/toches-arepas. Any member of the restaurant may connect with an
 * authco token (MCP clients get one through dynamic client registration); each sees only the
 * tools of their role, the same split as the panel.
 */
@Slf4j
@RestController
@RequestMapping("/mcp/{tenant}")
@RequiredArgsConstructor
@Tag(name = "MCP", description = "The restaurant for an AI assistant: sales, tables, orders, menu")
@SecurityRequirement(name = "bearerAuth")
public class McpController {

    static final List<String> PROTOCOL_VERSIONS = List.of("2025-06-18", "2025-03-26", "2024-11-05");

    private static final String INSTRUCTIONS = """
            Herramientas sobre el restaurante: ventas y estadísticas, mesas y cuentas abiertas, pedidos \
            en curso, domicilios y la carta. Responde en español y con pesos colombianos sin decimales ($15.000). \
            Las cifras salen de lo registrado en el sistema: no inventes datos. Antes de marcar un plato \
            o un ingrediente como agotado o disponible, confirma con la persona cuál es.""";

    private final TenantGateway tenantGateway;
    private final ResolveMemberUseCase resolveMember;
    private final RestaurantTools restaurantTools;
    private final JsonMapper json;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "JSON-RPC message (or batch) of the MCP protocol")
    public ResponseEntity<Object> handle(@PathVariable String tenant, @RequestBody Object body,
                                         @AuthenticationPrincipal Jwt jwt) {
        Tenant restaurant = TenantContext.asSystem(() -> tenantGateway.findBySlug(tenant.toLowerCase()))
                .orElse(null);
        if (restaurant == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        return TenantContext.as(restaurant, () -> {
            Optional<TenantMember> member = resolveMember.execute(jwt.getSubject(), jwt.getTokenValue());
            if (member.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).<Object>body(
                        error(null, -32001, "Only the team of " + restaurant.name() + " can use this server"));
            }
            TenantMember actor = member.get();
            if (body instanceof List<?> batch) {
                List<Object> responses = new ArrayList<>();
                for (Object message : batch) {
                    Map<String, Object> response = dispatch(message, actor, restaurant);
                    if (response != null) responses.add(response);
                }
                return responses.isEmpty() ? ResponseEntity.accepted().build() : ResponseEntity.ok(responses);
            }
            Map<String, Object> response = dispatch(body, actor, restaurant);
            return response == null ? ResponseEntity.accepted().build() : ResponseEntity.ok(response);
        });
    }

    /** No server-initiated stream: the spec allows 405 here. */
    @GetMapping
    public ResponseEntity<Void> stream() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> dispatch(Object raw, TenantMember actor, Tenant restaurant) {
        if (!(raw instanceof Map<?, ?> message) || !"2.0".equals(message.get("jsonrpc"))) {
            return error(null, -32600, "Invalid JSON-RPC request");
        }
        Object id = message.get("id");
        String method = String.valueOf(message.get("method"));
        // Notifications (no id) and responses get no answer
        if (id == null) return null;
        Map<String, Object> params = message.get("params") instanceof Map<?, ?> p ? (Map<String, Object>) p : Map.of();
        try {
            return switch (method) {
                case "initialize" -> result(id, initialize(params, restaurant));
                case "ping" -> result(id, Map.of());
                case "tools/list" -> result(id, Map.of("tools", restaurantTools.tools().stream()
                        .filter(t -> t.allows(actor)).map(McpTool::descriptor).toList()));
                case "tools/call" -> result(id, call(params, actor));
                default -> error(id, -32601, "Method not found: " + method);
            };
        } catch (RuntimeException e) {
            log.warn("MCP {} failed", method, e);
            return error(id, -32603, "Internal error");
        }
    }

    private Map<String, Object> initialize(Map<String, Object> params, Tenant restaurant) {
        Object requested = params.get("protocolVersion");
        String version = PROTOCOL_VERSIONS.contains(requested) ? (String) requested : PROTOCOL_VERSIONS.getFirst();
        return Map.of(
                "protocolVersion", version,
                "capabilities", Map.of("tools", Map.of("listChanged", false)),
                "serverInfo", Map.of("name", "restauco-" + restaurant.slug(), "title", restaurant.name(), "version", "1.0.0"),
                "instructions", INSTRUCTIONS);
    }

    /** Tool errors go back as results with isError, so the model can read and fix them. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> call(Map<String, Object> params, TenantMember actor) {
        String name = String.valueOf(params.get("name"));
        Map<String, Object> arguments = params.get("arguments") instanceof Map<?, ?> a ? (Map<String, Object>) a : Map.of();
        McpTool tool = restaurantTools.tools().stream().filter(t -> t.name().equals(name) && t.allows(actor))
                .findFirst().orElse(null);
        if (tool == null) return toolError("Unknown tool: " + name);
        try {
            Object value = tool.handler().apply(new McpArgs(arguments), actor);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content", List.of(Map.of("type", "text", "text", json.writeValueAsString(value))));
            result.put("isError", false);
            return result;
        } catch (DomainException e) {
            return toolError(e.getCode() + ": " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return toolError("INVALID_ARGUMENT: " + e.getMessage());
        }
    }

    private static Map<String, Object> toolError(String message) {
        return Map.of("content", List.of(Map.of("type", "text", "text", message)), "isError", true);
    }

    private static Map<String, Object> result(Object id, Object result) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("jsonrpc", "2.0");
        m.put("id", id);
        m.put("result", result);
        return m;
    }

    private static Map<String, Object> error(Object id, int code, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("jsonrpc", "2.0");
        m.put("id", id);
        m.put("error", Map.of("code", code, "message", message));
        return m;
    }
}
