package com.iflytek.skillhub.auth.oauth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;

/**
 * OAuth2 authorization request resolver that preserves a sanitized post-login redirect target in
 * the HTTP session. Also appends the IAM-mandated {@code oauth_timestamp} query parameter to
 * authorization requests for the corporate IAM registration.
 */
@Component
public class SkillHubOAuth2AuthorizationRequestResolver
        implements org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver {

    private final DefaultOAuth2AuthorizationRequestResolver delegate;
    private final OAuthLoginFlowService oauthLoginFlowService;

    public SkillHubOAuth2AuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository,
                                                      OAuthLoginFlowService oauthLoginFlowService) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository,
                "/oauth2/authorization"
        );
        this.oauthLoginFlowService = oauthLoginFlowService;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authorizationRequest = delegate.resolve(request);
        oauthLoginFlowService.rememberReturnTo(request);
        return applyProviderCustomizations(authorizationRequest);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authorizationRequest = delegate.resolve(request, clientRegistrationId);
        oauthLoginFlowService.rememberReturnTo(request);
        return applyProviderCustomizations(authorizationRequest);
    }

    /**
     * The corporate IAM authorization endpoint (spec section 3.3) requires a 13-digit
     * millisecond {@code oauth_timestamp} parameter that standard OAuth2 does not define.
     */
    private OAuth2AuthorizationRequest applyProviderCustomizations(OAuth2AuthorizationRequest authorizationRequest) {
        if (authorizationRequest == null) {
            return null;
        }
        Object registrationId = authorizationRequest.getAttribute(OAuth2ParameterNames.REGISTRATION_ID);
        if (!IamClaimsExtractor.PROVIDER.equals(registrationId)) {
            return authorizationRequest;
        }
        String timestamp = String.valueOf(System.currentTimeMillis());
        return OAuth2AuthorizationRequest.from(authorizationRequest)
                .authorizationRequestUri(
                        authorizationRequest.getAuthorizationRequestUri() + "&oauth_timestamp=" + timestamp)
                .additionalParameters(params -> params.put("oauth_timestamp", timestamp))
                .build();
    }
}
