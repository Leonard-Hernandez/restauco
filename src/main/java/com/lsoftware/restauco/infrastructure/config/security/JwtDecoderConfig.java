package com.lsoftware.restauco.infrastructure.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.List;

/**
 * authco access tokens (RS256). Keys come lazily from the internal authco URL, while the
 * issuer is validated against the public value and the audience against restauco's resource id.
 */
@Configuration
public class JwtDecoderConfig {

    @Bean
    JwtDecoder jwtDecoder(@Value("${restauco.security.internal-url}") String authcoInternalUrl,
                          @Value("${restauco.security.issuer}") String issuer,
                          @Value("${restauco.security.resource-id}") String resourceId) {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(authcoInternalUrl + "/oauth2/jwks").build();
        jwtDecoder.setJwtValidator(jwtValidator(issuer, resourceId));
        return jwtDecoder;
    }

    static OAuth2TokenValidator<Jwt> jwtValidator(String issuer, String resourceId) {
        return new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD, aud -> aud != null && aud.contains(resourceId)));
    }
}
