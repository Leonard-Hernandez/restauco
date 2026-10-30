package com.lsoftware.restauco.infrastructure.config.security;

import com.lsoftware.restauco.entity.member.model.MemberRole;
import com.lsoftware.restauco.infrastructure.config.tenant.TenantFilter;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String MCP_SESSION_ID = "Mcp-Session-Id";
    private static final String MCP_PROTOCOL_VERSION = "Mcp-Protocol-Version";

    private final PanelAuthorizationManager panel;

    @Value("${restauco.security.issuer}")
    private String issuer;

    @Value("${restauco.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // SSE streams finish on an async dispatch, after the request was authorized
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/**", "/media/**").permitAll()
                        // Guests order from the table QR without an account; a waiter approves it
                        .requestMatchers(HttpMethod.POST, "/api/public/tables/{token}/orders").permitAll()
                        // Delivery from the website: same idea, the waiter confirms it (and the address)
                        .requestMatchers(HttpMethod.POST, "/api/public/delivery/locate", "/api/public/delivery/orders").permitAll()
                        .requestMatchers("/api/panel/floor/**").access(panel.roles(MemberRole.WAITER))
                        .requestMatchers("/api/panel/kitchen/**").access(panel.roles(MemberRole.COOK))
                        .requestMatchers("/api/panel/admin/**").access(panel.roles())
                        .requestMatchers("/api/panel/**").access(panel.member())
                        // /mcp/{tenant} checks the member and role itself
                        .anyRequest().authenticated()
                )
                // RFC 9728: /.well-known/oauth-protected-resource[/<path>] and the resource_metadata
                // of every 401 come from Spring Security; MCP clients find authco there
                .oauth2ResourceServer(rs -> rs
                        .jwt(Customizer.withDefaults())
                        .protectedResourceMetadata(m -> m.protectedResourceMetadataCustomizer(meta -> meta
                                .authorizationServer(issuer)
                                .scope("openid").scope("profile").scope("email")
                                .resourceName("Restauco")
                                .tlsClientCertificateBoundAccessTokens(false))))
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(Arrays.stream(allowedOrigins.split(",")).map(String::strip).toList());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", TenantFilter.HEADER,
                MCP_SESSION_ID, MCP_PROTOCOL_VERSION));
        cfg.setExposedHeaders(List.of(HttpHeaders.WWW_AUTHENTICATE, MCP_SESSION_ID));
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
