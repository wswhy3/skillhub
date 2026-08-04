package com.iflytek.skillhub.service;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * MCP 对象 key 生成：mcp/{namespace}/{slug}/{artifactKey}/README.md
 * artifactKey 为随机不可预测段，只内部流转，不进公共响应。
 */
@Component
public class McpObjectKeyGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    public String newArtifactKey() {
        byte[] bytes = new byte[12];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public String readmeKey(String namespace, String slug, String artifactKey) {
        return "mcp/" + namespace + "/" + slug + "/" + artifactKey + "/README.md";
    }

    public String iconKey(String namespace, String slug, String artifactKey, String extension) {
        return "mcp/" + namespace + "/" + slug + "/" + artifactKey + "/icon" + extension;
    }

    public static String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
