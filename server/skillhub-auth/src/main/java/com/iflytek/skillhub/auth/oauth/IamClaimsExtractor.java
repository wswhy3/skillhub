package com.iflytek.skillhub.auth.oauth;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

/**
 * Claims extractor for the Changhong-Jiahua corporate IAM (CAS-style OAuth2).
 *
 * <p>The IAM profile endpoint returns a non-standard payload:
 * <pre>{@code
 * {
 *   "attributes": {
 *     "accountNo": "zhangsan",
 *     "userName": "张三",
 *     "personId": "12345",
 *     "employeeNumber": "90234",
 *     "token_expired": "7200",
 *     "token_gtime": 1621000246591
 *   },
 *   "id": "zhangsan"
 * }
 * }</pre>
 *
 * <p>Mapping decisions:
 * <ul>
 *   <li>{@code subject} = top-level {@code id}, falling back to {@code accountNo} — the stable
 *       IAM account identifier.</li>
 *   <li>{@code providerLogin} (used as the platform display name) = {@code userName} (Chinese
 *       name), falling back to {@code accountNo}/{@code id}.</li>
 *   <li>{@code email} is always {@code null} — the IAM profile does not expose email.</li>
 *   <li>{@code accountNo}/{@code employeeNumber}/{@code personId} are kept in {@code extra}.</li>
 * </ul>
 */
@Component
public class IamClaimsExtractor implements OAuthClaimsExtractor {

    /** Registration id / provider code shared by SecurityConfig, resolvers, and converters. */
    public static final String PROVIDER = "iam";

    private static final Logger log = LoggerFactory.getLogger(IamClaimsExtractor.class);

    @Override
    public String getProvider() {
        return PROVIDER;
    }

    @Override
    public OAuthClaims extract(OAuth2UserRequest request, OAuth2User oAuth2User) {
        Map<String, Object> attrs = oAuth2User.getAttributes();

        Map<String, Object> profile = Map.of();
        if (attrs.get("attributes") instanceof Map<?, ?> nested) {
            profile = new HashMap<>();
            for (Map.Entry<?, ?> entry : nested.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    profile.put(key, entry.getValue());
                }
            }
        }

        String accountNo = stringValue(profile.get("accountNo"));
        String userName = stringValue(profile.get("userName"));
        String id = stringValue(attrs.get("id"));

        String subject = firstNonBlank(id, accountNo);
        if (subject == null) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user_info_response",
                            "IAM profile missing both id and accountNo", null)
            );
        }

        String displayName = firstNonBlank(userName, accountNo, id);

        Map<String, Object> extra = new HashMap<>();
        if (accountNo != null) {
            extra.put("accountNo", accountNo);
        }
        putIfPresent(extra, "employeeNumber", profile.get("employeeNumber"));
        putIfPresent(extra, "personId", profile.get("personId"));

        log.info("IAM OAuth claims extracted - subject: {}, displayName: {}", subject, displayName);

        return new OAuthClaims(PROVIDER, subject, null, false, displayName, extra);
    }

    private static void putIfPresent(Map<String, Object> target, String key, Object value) {
        String text = stringValue(value);
        if (text != null) {
            target.put(key, text);
        }
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? null : text;
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return null;
    }
}
