package com.iflytek.skillhub.auth.oauth;

import java.net.URI;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequestEntityConverter;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Builds the user-info request for OAuth providers.
 *
 * <p>For the corporate IAM ({@link IamClaimsExtractor#PROVIDER}), the spec (section 3.5) requires
 * the access token to be passed as an {@code access_token} query parameter instead of the standard
 * {@code Authorization: Bearer} header. All other providers fall through to Spring Security's
 * default request converter.
 */
public class IamUserInfoRequestEntityConverter implements Converter<OAuth2UserRequest, RequestEntity<?>> {

    private final OAuth2UserRequestEntityConverter delegate = new OAuth2UserRequestEntityConverter();

    @Override
    public RequestEntity<?> convert(OAuth2UserRequest userRequest) {
        if (!IamClaimsExtractor.PROVIDER.equals(userRequest.getClientRegistration().getRegistrationId())) {
            return delegate.convert(userRequest);
        }

        URI uri = UriComponentsBuilder
                .fromUriString(userRequest.getClientRegistration().getProviderDetails()
                        .getUserInfoEndpoint().getUri())
                .queryParam("access_token", userRequest.getAccessToken().getTokenValue())
                .build()
                .encode()
                .toUri();

        return RequestEntity.get(uri)
                .accept(MediaType.APPLICATION_JSON)
                .build();
    }
}
