package com.lsoftware.restauco.infrastructure.config.authco;

import com.lsoftware.restauco.entity.exception.DomainException;
import com.lsoftware.restauco.entity.identity.gateway.IdentityGateway;
import com.lsoftware.restauco.entity.identity.model.UserIdentity;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AuthcoIdentityGateway implements IdentityGateway {

    private final RestClient restClient;

    public AuthcoIdentityGateway(RestClient.Builder builder,
                                 @Value("${restauco.security.internal-url}") String authcoInternalUrl) {
        this.restClient = builder.baseUrl(authcoInternalUrl).build();
    }

    @Override
    public UserIdentity userInfo(String accessToken) {
        try {
            UserInfo info = restClient.get()
                    .uri("/userinfo")
                    .headers(h -> h.setBearerAuth(accessToken))
                    .retrieve()
                    .body(UserInfo.class);
            if (info == null) throw unavailable();
            return new UserIdentity(info.sub(), info.email(), info.name(), Boolean.TRUE.equals(info.emailVerified()));
        } catch (RestClientException e) {
            throw unavailable();
        }
    }

    private static DomainException unavailable() {
        return new DomainException("IDENTITY_UNAVAILABLE", "Could not read the user profile from authco");
    }

    record UserInfo(String sub, String email, String name, @JsonProperty("email_verified") Boolean emailVerified) {}
}
