package com.iflytek.skillhub.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

class IamUserInfoRequestEntityConverterTest {

    private final IamUserInfoRequestEntityConverter converter = new IamUserInfoRequestEntityConverter();

    @Test
    void convert_sendsAccessTokenAsQueryParamForIam() {
        RequestEntity<?> entity = converter.convert(userRequest("iam",
                "https://iam.example.com/esc-sso/oauth2.0/profile"));

        assertThat(entity.getMethod()).isEqualTo(HttpMethod.GET);
        assertThat(entity.getUrl().toString())
                .isEqualTo("https://iam.example.com/esc-sso/oauth2.0/profile?access_token=AT-30-token");
        assertThat(entity.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).isNull();
    }

    @Test
    void convert_fallsBackToBearerHeaderForOtherProviders() {
        RequestEntity<?> entity = converter.convert(userRequest("gitlab",
                "https://gitlab.example.com/api/v4/user"));

        assertThat(entity.getMethod()).isEqualTo(HttpMethod.GET);
        assertThat(entity.getUrl().toString()).isEqualTo("https://gitlab.example.com/api/v4/user");
        assertThat(entity.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer AT-30-token");
    }

    private OAuth2UserRequest userRequest(String registrationId, String userInfoUri) {
        ClientRegistration registration = ClientRegistration.withRegistrationId(registrationId)
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://example.com/authorize")
                .tokenUri("https://example.com/token")
                .userInfoUri(userInfoUri)
                .userNameAttributeName("id")
                .clientName(registrationId)
                .build();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "AT-30-token",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );
        return new OAuth2UserRequest(registration, accessToken);
    }
}
