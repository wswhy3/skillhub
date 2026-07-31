package com.iflytek.skillhub.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.web.util.UriComponentsBuilder;

class IamTokenRequestEntityConverterTest {

    private final IamTokenRequestEntityConverter converter = new IamTokenRequestEntityConverter();

    @Test
    void convert_buildsPostWithAllParametersInQueryString() {
        RequestEntity<?> entity = converter.convert(grantRequest());

        assertThat(entity.getMethod()).isEqualTo(HttpMethod.POST);
        assertThat(entity.hasBody()).isFalse();

        String url = entity.getUrl().toString();
        assertThat(url).startsWith("https://iam.example.com/esc-sso/oauth2.0/accessToken?");

        Map<String, String> params = UriComponentsBuilder.fromUri(entity.getUrl())
                .build()
                .getQueryParams()
                .toSingleValueMap();
        assertThat(params)
                .containsEntry("grant_type", "authorization_code")
                .containsEntry("client_id", "app-key")
                .containsEntry("client_secret", "app-secret")
                .containsEntry("code", "auth-code-1")
                .containsEntry("redirect_uri", "https://skillhub.example.com/login/oauth2/code/iam");

        String timestamp = params.get("oauth_timestamp");
        assertThat(timestamp).isNotNull();
        assertThat(timestamp).hasSize(13);
        assertThat(Long.parseLong(timestamp)).isPositive();
    }

    private OAuth2AuthorizationCodeGrantRequest grantRequest() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("iam")
                .clientId("app-key")
                .clientSecret("app-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://skillhub.example.com/login/oauth2/code/iam")
                .authorizationUri("https://iam.example.com/esc-sso/oauth2.0/authorize")
                .tokenUri("https://iam.example.com/esc-sso/oauth2.0/accessToken")
                .userInfoUri("https://iam.example.com/esc-sso/oauth2.0/profile")
                .userNameAttributeName("id")
                .clientName("IAM")
                .build();

        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri(registration.getProviderDetails().getAuthorizationUri())
                .clientId(registration.getClientId())
                .redirectUri(registration.getRedirectUri())
                .state("state-1")
                .build();
        OAuth2AuthorizationResponse authorizationResponse = OAuth2AuthorizationResponse.success("auth-code-1")
                .redirectUri(registration.getRedirectUri())
                .state("state-1")
                .build();

        return new OAuth2AuthorizationCodeGrantRequest(
                registration,
                new OAuth2AuthorizationExchange(authorizationRequest, authorizationResponse)
        );
    }
}
