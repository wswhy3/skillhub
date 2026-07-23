package com.iflytek.skillhub.auth.oauth;

import java.net.URI;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Builds the authorization-code token request for the corporate IAM.
 *
 * <p>Unlike standard OAuth2 token endpoints that expect form parameters in the POST body, the
 * IAM spec (section 3.4) requires all parameters — including a mandatory 13-digit
 * {@code oauth_timestamp} — to be sent as URL query parameters on a POST request.
 */
public class IamTokenRequestEntityConverter
        implements Converter<OAuth2AuthorizationCodeGrantRequest, RequestEntity<?>> {

    @Override
    public RequestEntity<?> convert(OAuth2AuthorizationCodeGrantRequest grantRequest) {
        ClientRegistration registration = grantRequest.getClientRegistration();
        String code = grantRequest.getAuthorizationExchange().getAuthorizationResponse().getCode();
        String redirectUri = grantRequest.getAuthorizationExchange().getAuthorizationRequest()
                .getRedirectUri();

        URI uri = UriComponentsBuilder.fromUriString(registration.getProviderDetails().getTokenUri())
                .queryParam("grant_type", "authorization_code")
                .queryParam("oauth_timestamp", String.valueOf(System.currentTimeMillis()))
                .queryParam("client_id", registration.getClientId())
                .queryParam("client_secret", registration.getClientSecret())
                .queryParam("code", code)
                .queryParam("redirect_uri", redirectUri)
                .build()
                .encode()
                .toUri();

        return RequestEntity.post(uri)
                .accept(MediaType.APPLICATION_JSON)
                .build();
    }
}
