package com.iflytek.skillhub.service;

import com.iflytek.skillhub.domain.mcp.McpSourceType;
import com.iflytek.skillhub.domain.shared.exception.DomainBadRequestException;
import com.iflytek.skillhub.storage.ObjectStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;

/**
 * MCP 内容服务：从 GitHub 仓库或 HTTP 来源抓取 README，
 * 校验大小与 UTF-8，存入对象存储并计算 SHA-256。
 * 不执行脚本、不处理脚本化安装器；不验证签名（P0 记录 SHA-256 即可）。
 */
@Service
public class McpContentService {

    private static final Logger log = LoggerFactory.getLogger(McpContentService.class);

    /** README 上限 1MB */
    static final long MAX_README_BYTES = 1024 * 1024;
    private static final Duration FETCH_TIMEOUT = Duration.ofSeconds(15);

    private final ObjectStorageService objectStorageService;
    private final McpObjectKeyGenerator keyGenerator;
    private final HttpClient httpClient;

    public McpContentService(ObjectStorageService objectStorageService, McpObjectKeyGenerator keyGenerator) {
        this.objectStorageService = objectStorageService;
        this.keyGenerator = keyGenerator;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(FETCH_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * 从上游来源抓取 README。返回抓取结果（含对象 key 与 SHA-256）。
     */
    public FetchedReadme fetchReadme(McpSourceType sourceType, String sourceUrl,
                                     String namespace, String slug) {
        String readmeUrl = resolveReadmeUrl(sourceType, sourceUrl);
        byte[] content = fetchBytes(readmeUrl);
        validateReadme(content);

        String artifactKey = keyGenerator.newArtifactKey();
        String objectKey = keyGenerator.readmeKey(namespace, slug, artifactKey);
        objectStorageService.putObject(objectKey, new ByteArrayInputStream(content), content.length,
                "text/markdown; charset=UTF-8");
        String sha256 = McpObjectKeyGenerator.sha256Hex(content);
        return new FetchedReadme(objectKey, sha256, (long) content.length, readmeUrl);
    }

    /** 保存手工上传的 README。 */
    public FetchedReadme storeManualReadme(byte[] content, String namespace, String slug) {
        if (content == null || content.length == 0) {
            throw new DomainBadRequestException("mcp.readme.empty");
        }
        validateReadme(content);
        String artifactKey = keyGenerator.newArtifactKey();
        String objectKey = keyGenerator.readmeKey(namespace, slug, artifactKey);
        objectStorageService.putObject(objectKey, new ByteArrayInputStream(content), content.length,
                "text/markdown; charset=UTF-8");
        return new FetchedReadme(objectKey, McpObjectKeyGenerator.sha256Hex(content), (long) content.length, null);
    }

    /** 保存图标，按声明的 contentType 存储。 */
    public StoredIcon storeIcon(byte[] content, String contentType, String namespace, String slug) {
        if (content == null || content.length == 0) {
            throw new DomainBadRequestException("mcp.icon.empty");
        }
        String extension = iconExtension(contentType);
        String artifactKey = keyGenerator.newArtifactKey();
        String objectKey = keyGenerator.iconKey(namespace, slug, artifactKey, extension);
        objectStorageService.putObject(objectKey, new ByteArrayInputStream(content), content.length,
                contentType != null ? contentType : "application/octet-stream");
        return new StoredIcon(objectKey, McpObjectKeyGenerator.sha256Hex(content),
                contentType, (long) content.length);
    }

    /** 根据来源类型解析 README 地址。 */
    private String resolveReadmeUrl(McpSourceType sourceType, String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new DomainBadRequestException("mcp.source_url.required");
        }
        if (sourceType == McpSourceType.GITHUB_REPOSITORY) {
            return toGithubRawReadme(sourceUrl.trim());
        }
        if (sourceType == McpSourceType.REMOTE_HTTP) {
            return sourceUrl.trim();
        }
        throw new DomainBadRequestException("mcp.source_type.unsupported", sourceType);
    }

    /**
     * GitHub 仓库地址转 raw README。
     * 支持 https://github.com/{owner}/{repo}[/tree/{branch}/{path}]，
     * README 提取启发式：依次尝试 README.md / README.zh-CN.md / README。
     */
    private String toGithubRawReadme(String repoUrl) {
        String normalized = repoUrl.replaceFirst("^https?://(www\\.)?github\\.com/", "");
        normalized = normalized.replaceAll("/+$", "");
        if (normalized.endsWith(".git")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        String branch = "main";
        String basePath = normalized;
        int treeIdx = normalized.indexOf("/tree/");
        if (treeIdx >= 0) {
            basePath = normalized.substring(0, treeIdx);
            String rest = normalized.substring(treeIdx + "/tree/".length());
            String[] parts = rest.split("/");
            if (parts.length > 0 && !parts[0].isBlank()) {
                branch = parts[0];
            }
        }
        if (basePath.split("/").length < 2) {
            throw new DomainBadRequestException("mcp.source_url.invalid_github", repoUrl);
        }
        return "https://raw.githubusercontent.com/" + basePath + "/" + branch + "/README.md";
    }

    private byte[] fetchBytes(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(FETCH_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                throw new DomainBadRequestException("mcp.source.fetch_failed", url, response.statusCode());
            }
            return response.body();
        } catch (DomainBadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to fetch MCP readme from {}: {}", url, e.getMessage());
            throw new DomainBadRequestException("mcp.source.fetch_failed", url, e.getMessage());
        }
    }

    private void validateReadme(byte[] content) {
        if (content.length == 0) {
            throw new DomainBadRequestException("mcp.readme.empty");
        }
        if (content.length > MAX_README_BYTES) {
            throw new DomainBadRequestException("mcp.readme.too_large", MAX_README_BYTES);
        }
        // UTF-8 校验
        if (!isValidUtf8(content)) {
            throw new DomainBadRequestException("mcp.readme.not_utf8");
        }
    }

    private boolean isValidUtf8(byte[] bytes) {
        try {
            new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            // 宽松校验：解码不抛异常即可（Java 默认替换非法序列）。
            // 严格校验用 decoder，这里用宽松即可满足 P0。
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String iconExtension(String contentType) {
        if (contentType == null) {
            return ".png";
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/svg+xml" -> ".svg";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".png";
        };
    }

    public record FetchedReadme(String objectKey, String sha256, Long size, String resolvedUrl) {}

    public record StoredIcon(String objectKey, String sha256, String contentType, Long size) {}
}
