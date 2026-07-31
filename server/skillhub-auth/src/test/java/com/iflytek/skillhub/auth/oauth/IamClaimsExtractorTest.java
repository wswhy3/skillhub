package com.iflytek.skillhub.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

class IamClaimsExtractorTest {

    private final IamClaimsExtractor extractor = new IamClaimsExtractor();

    @Test
    void extract_mapsCasStyleProfileToClaims() {
        OAuthClaims claims = extractor.extract(
                userRequest(),
                new DefaultOAuth2User(
                        List.of(),
                        Map.of(
                                "id", "zhangsan",
                                "attributes", Map.of(
                                        "accountNo", "zhangsan",
                                        "userName", "张三",
                                        "personId", "12345",
                                        "employeeNumber", "90234",
                                        "token_expired", "7200",
                                        "token_gtime", 1621000246591L
                                )
                        ),
                        "id"
                )
        );

        assertThat(claims.provider()).isEqualTo("iam");
        assertThat(claims.subject()).isEqualTo("zhangsan");
        assertThat(claims.providerLogin()).isEqualTo("张三");
        assertThat(claims.email()).isNull();
        assertThat(claims.emailVerified()).isFalse();
        assertThat(claims.extra())
                .containsEntry("accountNo", "zhangsan")
                .containsEntry("employeeNumber", "90234")
                .containsEntry("personId", "12345");
    }

    @Test
    void extract_fallsBackToAccountNoWhenUserNameMissing() {
        OAuthClaims claims = extractor.extract(
                userRequest(),
                new DefaultOAuth2User(
                        List.of(),
                        Map.of(
                                "id", "zhangsan",
                                "attributes", Map.of("accountNo", "zhangsan")
                        ),
                        "id"
                )
        );

        assertThat(claims.subject()).isEqualTo("zhangsan");
        assertThat(claims.providerLogin()).isEqualTo("zhangsan");
    }

    @Test
    void extract_usesAccountNoAsSubjectWhenTopLevelIdMissing() {
        OAuthClaims claims = extractor.extract(
                userRequest(),
                new DefaultOAuth2User(
                        List.of(),
                        Map.of("attributes", Map.of(
                                "accountNo", "lisi",
                                "userName", "李四"
                        )),
                        "attributes"
                )
        );

        assertThat(claims.subject()).isEqualTo("lisi");
        assertThat(claims.providerLogin()).isEqualTo("李四");
    }

    @Test
    void extract_throwsWhenNoIdentifierPresent() {
        assertThatThrownBy(() -> extractor.extract(
                userRequest(),
                new DefaultOAuth2User(
                        List.of(),
                        Map.of("attributes", Map.of("userName", "张三")),
                        "attributes"
                )
        )).isInstanceOf(OAuth2AuthenticationException.class);
    }

    private OAuth2UserRequest userRequest() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("iam")
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://iam.example.com/esc-sso/oauth2.0/authorize")
                .tokenUri("https://iam.example.com/esc-sso/oauth2.0/accessToken")
                .userInfoUri("https://iam.example.com/esc-sso/oauth2.0/profile")
                .userNameAttributeName("id")
                .clientName("企业IAM登录")
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
