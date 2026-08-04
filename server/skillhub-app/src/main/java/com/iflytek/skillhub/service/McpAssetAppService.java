package com.iflytek.skillhub.service;

import com.iflytek.skillhub.domain.mcp.McpServer;
import com.iflytek.skillhub.domain.mcp.McpServerRepository;
import com.iflytek.skillhub.domain.mcp.McpServerStatus;
import com.iflytek.skillhub.domain.mcp.McpVersion;
import com.iflytek.skillhub.domain.mcp.McpVersionRepository;
import com.iflytek.skillhub.domain.mcp.McpVisibility;
import com.iflytek.skillhub.domain.namespace.Namespace;
import com.iflytek.skillhub.domain.namespace.NamespaceRepository;
import com.iflytek.skillhub.storage.ObjectStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.Optional;

/**
 * MCP 静态资源（README/图标）读取：从对象存储按 key 取内容，
 * 不向浏览器暴露内部 key，ETag 使用内容 SHA-256。
 */
@Service
public class McpAssetAppService {

    private static final String GLOBAL_NAMESPACE_SLUG = "global";

    private final McpServerRepository serverRepository;
    private final McpVersionRepository versionRepository;
    private final NamespaceRepository namespaceRepository;
    private final ObjectStorageService objectStorageService;

    public McpAssetAppService(McpServerRepository serverRepository,
                              McpVersionRepository versionRepository,
                              NamespaceRepository namespaceRepository,
                              ObjectStorageService objectStorageService) {
        this.serverRepository = serverRepository;
        this.versionRepository = versionRepository;
        this.namespaceRepository = namespaceRepository;
        this.objectStorageService = objectStorageService;
    }

    /** 读取当前发布版本的 README。返回 null 表示不可见/不存在。 */
    @Transactional(readOnly = true)
    public AssetContent loadReadme(String namespaceSlug, String slug) {
        McpVersion version = resolvePublishedVersion(namespaceSlug, slug);
        if (version == null || version.getReadmeObjectKey() == null) {
            return null;
        }
        InputStream stream = objectStorageService.getObject(version.getReadmeObjectKey());
        return new AssetContent(stream, "text/markdown; charset=UTF-8", version.getReadmeSha256(),
                version.getReadmeSize());
    }

    /** 读取指定草稿版本的 README，供后台修订时回填编辑器。 */
    @Transactional(readOnly = true)
    public AssetContent loadDraftReadme(Long versionId) {
        McpVersion version = versionRepository.findById(versionId).orElse(null);
        if (version == null || version.getReadmeObjectKey() == null) {
            return null;
        }
        InputStream stream = objectStorageService.getObject(version.getReadmeObjectKey());
        return new AssetContent(stream, "text/markdown; charset=UTF-8", version.getReadmeSha256(),
                version.getReadmeSize());
    }

    @Transactional(readOnly = true)
    public AssetContent loadIcon(String namespaceSlug, String slug) {
        McpVersion version = resolvePublishedVersion(namespaceSlug, slug);
        if (version == null || version.getIconObjectKey() == null) {
            return null;
        }
        InputStream stream = objectStorageService.getObject(version.getIconObjectKey());
        String contentType = version.getIconContentType() != null ? version.getIconContentType() : "application/octet-stream";
        return new AssetContent(stream, contentType, version.getIconSha256(), version.getIconSize());
    }

    /** 公共可见性过滤：容器 ACTIVE、未隐藏、PUBLIC、latest 非空。 */
    private McpVersion resolvePublishedVersion(String namespaceSlug, String slug) {
        String effectiveNs = (namespaceSlug == null || namespaceSlug.isBlank()) ? GLOBAL_NAMESPACE_SLUG : namespaceSlug;
        Optional<Namespace> namespace = namespaceRepository.findBySlug(effectiveNs);
        if (namespace.isEmpty()) {
            return null;
        }
        McpServer server = serverRepository.findByNamespaceIdAndSlug(namespace.get().getId(), slug).orElse(null);
        if (server == null || server.getLatestVersionId() == null) {
            return null;
        }
        if (server.isHidden() || server.getStatus() != McpServerStatus.ACTIVE
                || server.getVisibility() != McpVisibility.PUBLIC) {
            return null;
        }
        return versionRepository.findById(server.getLatestVersionId()).orElse(null);
    }

    public record AssetContent(InputStream stream, String contentType, String sha256, Long size) {}
}
