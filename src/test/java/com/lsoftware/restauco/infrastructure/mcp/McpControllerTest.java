package com.lsoftware.restauco.infrastructure.mcp;

import com.lsoftware.restauco.entity.exception.DomainException;
import com.lsoftware.restauco.entity.member.model.MemberRole;
import com.lsoftware.restauco.entity.member.model.TenantMember;
import com.lsoftware.restauco.entity.tenant.gateway.TenantGateway;
import com.lsoftware.restauco.entity.tenant.model.Tenant;
import com.lsoftware.restauco.usecase.member.ResolveMemberUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class McpControllerTest {

    private static final Tenant TOCHES = new Tenant(1L, "toches-arepas", "Toche's Arepas", ZoneId.of("America/Bogota"),
            null, null, Map.of());

    private final TenantGateway tenants = mock(TenantGateway.class);
    private final ResolveMemberUseCase members = mock(ResolveMemberUseCase.class);
    private final RestaurantTools tools = mock(RestaurantTools.class);
    private final McpController controller = new McpController(tenants, members, tools, JsonMapper.builder().build());
    private final Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject("sub-ana")
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();

    @BeforeEach
    void setUp() {
        when(tenants.findBySlug("toches-arepas")).thenReturn(Optional.of(TOCHES));
        member(MemberRole.OWNER);
        when(tools.tools()).thenReturn(List.of(
                new McpTool("echo", "Echo", "Returns its input", Schemas.object().string("text", "Text", true).build(),
                        Set.of(MemberRole.OWNER, MemberRole.WAITER), true,
                        (args, member) -> Map.of("echo", args.requiredString("text"), "role", member.role().name())),
                new McpTool("sales", "Sales", "Owner only", Schemas.object().build(),
                        Set.of(MemberRole.OWNER), true, (args, member) -> Map.of("total", 1)),
                new McpTool("fails", "Fails", "Always fails", Schemas.object().build(),
                        Set.of(MemberRole.OWNER), true,
                        (args, member) -> { throw new DomainException("TABLE_NOT_FOUND", "Table not found"); })));
    }

    @Test
    @SuppressWarnings("unchecked")
    void initializeNegotiatesTheProtocolVersion() {
        var response = (Map<String, Object>) controller.handle("toches-arepas",
                rpc(1, "initialize", Map.of("protocolVersion", "2025-03-26")), jwt).getBody();

        var result = (Map<String, Object>) response.get("result");
        assertThat(result.get("protocolVersion")).isEqualTo("2025-03-26");
        assertThat(result).containsKey("instructions");
        assertThat((Map<String, Object>) result.get("serverInfo")).containsEntry("title", "Toche's Arepas");
    }

    @Test
    void notificationsAreAcceptedWithoutBody() {
        var response = controller.handle("toches-arepas", Map.of("jsonrpc", "2.0", "method", "notifications/initialized"), jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void toolCallsRunAsTheMember() {
        var response = (Map<String, Object>) controller.handle("toches-arepas",
                rpc(2, "tools/call", Map.of("name", "echo", "arguments", Map.of("text", "hola"))), jwt).getBody();

        var result = (Map<String, Object>) response.get("result");
        assertThat(result.get("isError")).isEqualTo(false);
        assertThat(((List<Map<String, Object>>) result.get("content")).getFirst().get("text").toString())
                .contains("\"echo\":\"hola\"").contains("\"role\":\"OWNER\"");
    }

    @Test
    @SuppressWarnings("unchecked")
    void eachRoleOnlySeesAndCallsItsTools() {
        member(MemberRole.WAITER);

        var list = (Map<String, Object>) controller.handle("toches-arepas", rpc(3, "tools/list", Map.of()), jwt).getBody();
        var listed = (List<Map<String, Object>>) ((Map<String, Object>) list.get("result")).get("tools");
        assertThat(listed).extracting(t -> t.get("name")).containsExactly("echo");

        var call = (Map<String, Object>) controller.handle("toches-arepas",
                rpc(4, "tools/call", Map.of("name", "sales")), jwt).getBody();
        assertThat(((Map<String, Object>) call.get("result")).get("isError")).isEqualTo(true);
    }

    @Test
    @SuppressWarnings("unchecked")
    void domainErrorsComeBackAsToolErrorsTheModelCanRead() {
        var response = (Map<String, Object>) controller.handle("toches-arepas",
                rpc(5, "tools/call", Map.of("name", "fails")), jwt).getBody();

        var result = (Map<String, Object>) response.get("result");
        assertThat(result.get("isError")).isEqualTo(true);
        assertThat(result.toString()).contains("TABLE_NOT_FOUND");
    }

    @Test
    void strangersCannotUseIt() {
        when(members.execute("sub-ana", "token")).thenReturn(Optional.empty());

        assertThat(controller.handle("toches-arepas", rpc(6, "tools/list", Map.of()), jwt).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void unknownRestaurantsAreNotFound() {
        when(tenants.findBySlug("otro")).thenReturn(Optional.empty());

        assertThat(controller.handle("otro", rpc(7, "tools/list", Map.of()), jwt).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private void member(MemberRole role) {
        when(members.execute("sub-ana", "token")).thenReturn(Optional.of(
                new TenantMember(1L, "ana@example.com", "sub-ana", "Ana", role)));
    }

    private static Map<String, Object> rpc(int id, String method, Map<String, Object> params) {
        return Map.of("jsonrpc", "2.0", "id", id, "method", method, "params", params);
    }
}
