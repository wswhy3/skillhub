package com.iflytek.skillhub.service;

import com.iflytek.skillhub.domain.mcp.McpServer;
import com.iflytek.skillhub.domain.mcp.McpServerRepository;
import com.iflytek.skillhub.domain.mcp.McpSourceType;
import com.iflytek.skillhub.domain.mcp.McpVersion;
import com.iflytek.skillhub.domain.mcp.McpVersionCategoryRepository;
import com.iflytek.skillhub.domain.mcp.McpVersionCategory;
import com.iflytek.skillhub.domain.mcp.service.McpDraftService;
import com.iflytek.skillhub.domain.namespace.Namespace;
import com.iflytek.skillhub.domain.namespace.NamespaceRepository;
import com.iflytek.skillhub.domain.shared.exception.DomainBadRequestException;
import com.iflytek.skillhub.dto.McpDraftRequest;
import com.iflytek.skillhub.dto.McpDraftResponse;
import com.iflytek.skillhub.dto.McpImportRequest;
import com.iflytek.skillhub.dto.McpManualReadmeRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * MCP 运营后台-草稿与导入编排。
 */
@Service
public class McpAdminDraftAppService {

    private static final String GLOBAL_NAMESPACE_SLUG = "global";

    private final McpDraftService draftService;
    private final McpServerRepository serverRepository;
    private final McpVersionCategoryRepository versionCategoryRepository;
    private final NamespaceRepository namespaceRepository;
    private final McpContentService contentService;
    private final McpAdminAuditService auditService;

    public McpAdminDraftAppService(McpDraftService draftService,
                                   McpServerRepository serverRepository,
                                   McpVersionCategoryRepository versionCategoryRepository,
                                   NamespaceRepository namespaceRepository,
                                   McpContentService contentService,
                                   McpAdminAuditService auditService) {
        this.draftService = draftService;
        this.serverRepository = serverRepository;
        this.versionCategoryRepository = versionCategoryRepository;
        this.namespaceRepository = namespaceRepository;
        this.contentService = contentService;
        this.auditService = auditService;
    }

    @Transactional
    public McpDraftResponse createDraft(McpDraftRequest request, String operator,
                                        String clientIp, String userAgent) {
        Namespace global = requireGlobalNamespace();
        String slug = normalizeSlug(request.slug());

        McpServer server = serverRepository.findByNamespaceIdAndSlug(global.getId(), slug)
                .orElseGet(() -> {
                    try {
                        return serverRepository.save(new McpServer(global.getId(), slug, operator,
                                com.iflytek.skillhub.domain.mcp.McpVisibility.PUBLIC));
                    } catch (DataIntegrityViolationException e) {
                        throw new DomainBadRequestException("mcp.slug.duplicate", slug);
                    }
                });

        McpVersion draft = draftService.createDraft(server, request.displayName(), request.summary(),
                McpSourceType.MANUAL, operator);
        applyMetadata(draft, request);
        replaceCategories(draft.getId(), request.categoryIds());

        auditService.record(operator, "MCP_DRAFT_CREATE", draft.getId(),
                auditService.detail("slug", slug, "revisionNo", draft.getRevisionNo()), clientIp, userAgent);
        return toResponse(draft, server, global.getSlug());
    }

    @Transactional
    public McpDraftResponse updateDraft(Long versionId, McpDraftRequest request, String operator,
                                        String clientIp, String userAgent) {
        McpVersion version = draftService.updateDraft(versionId, operator, toMetadata(request));
        replaceCategories(versionId, request.categoryIds());
        McpServer server = draftService.requireServer(version.getServerId());
        Namespace global = requireGlobalNamespace();

        auditService.record(operator, "MCP_DRAFT_UPDATE", versionId,
                auditService.detail("slug", server.getSlug(), "revisionNo", version.getRevisionNo()),
                clientIp, userAgent);
        return toResponse(version, server, global.getSlug());
    }

    @Transactional
    public McpDraftResponse createRevisionDraft(Long sourceVersionId, String operator,
                                                String clientIp, String userAgent) {
        McpVersion draft = draftService.copyAsNewDraft(sourceVersionId, operator);
        if (versionCategoryRepository.findByVersionId(draft.getId()).isEmpty()) {
            copyCategories(sourceVersionId, draft.getId());
        }

        McpServer server = draftService.requireServer(draft.getServerId());
        Namespace global = requireGlobalNamespace();
        auditService.record(operator, "MCP_DRAFT_REVISION_CREATE", draft.getId(),
                auditService.detail("slug", server.getSlug(), "sourceVersionId", sourceVersionId,
                        "revisionNo", draft.getRevisionNo()),
                clientIp, userAgent);
        return toResponse(draft, server, global.getSlug());
    }

    @Transactional(readOnly = true)
    public McpDraftResponse getDraft(Long versionId) {
        McpVersion version = draftService.requireVersion(versionId);
        McpServer server = draftService.requireServer(version.getServerId());
        Namespace global = requireGlobalNamespace();
        return toResponse(version, server, global.getSlug());
    }

    /** 通过上游来源导入元数据并抓取 README。 */
    @Transactional
    public McpDraftResponse importDraft(Long versionId, McpImportRequest request, String operator,
                                        String clientIp, String userAgent) {
        McpVersion version = draftService.requireVersion(versionId);
        McpServer server = draftService.requireServer(version.getServerId());
        Namespace global = requireGlobalNamespace();
        McpSourceType sourceType = parseSourceType(request.sourceType());

        version.setSourceType(sourceType);
        version.setSourceUrl(request.sourceUrl());
        version.setSourceRevision(request.sourceRevision());
        version.setSourceFetchedAt(Instant.now(Clock.systemUTC()));

        try {
            McpContentService.FetchedReadme readme = contentService.fetchReadme(
                    sourceType, request.sourceUrl(), global.getSlug(), server.getSlug());
            version.markContentReady(readme.objectKey(), readme.sha256(), readme.size());
            version.setSourceSha256(readme.sha256());
        } catch (DomainBadRequestException e) {
            version.markContentFailed("FETCH_FAILED", e.getMessage());
            draftService.save(version);
            auditService.record(operator, "MCP_IMPORT_FAIL", versionId,
                    auditService.detail("slug", server.getSlug(), "sourceUrl", request.sourceUrl(),
                            "error", e.getMessage()), clientIp, userAgent);
            throw e;
        }
        draftService.save(version);

        auditService.record(operator, "MCP_IMPORT_REQUEST", versionId,
                auditService.detail("slug", server.getSlug(), "sourceUrl", request.sourceUrl(),
                        "sourceType", sourceType.name()), clientIp, userAgent);
        return toResponse(version, server, global.getSlug());
    }

    /** 手工填写 README 并保存为当前草稿内容。 */
    @Transactional
    public McpDraftResponse saveManualReadme(Long versionId, McpManualReadmeRequest request, String operator,
                                            String clientIp, String userAgent) {
        McpVersion version = draftService.requireVersion(versionId);
        McpServer server = draftService.requireServer(version.getServerId());
        Namespace global = requireGlobalNamespace();

        version.setSourceType(McpSourceType.MANUAL);
        version.setSourceUrl(null);
        version.setSourceRevision(null);
        version.setSourceFetchedAt(Instant.now(Clock.systemUTC()));

        try {
            McpContentService.FetchedReadme readme = contentService.storeManualReadme(
                    request.readme() == null ? null : request.readme().getBytes(StandardCharsets.UTF_8),
                    global.getSlug(),
                    server.getSlug());
            version.markContentReady(readme.objectKey(), readme.sha256(), readme.size());
            version.setSourceSha256(readme.sha256());
        } catch (DomainBadRequestException e) {
            version.markContentFailed("MANUAL_README_INVALID", e.getMessage());
            draftService.save(version);
            auditService.record(operator, "MCP_MANUAL_README_FAIL", versionId,
                    auditService.detail("slug", server.getSlug(), "error", e.getMessage()), clientIp, userAgent);
            throw e;
        }
        draftService.save(version);

        auditService.record(operator, "MCP_MANUAL_README_SAVE", versionId,
                auditService.detail("slug", server.getSlug(), "sourceType", McpSourceType.MANUAL.name()),
                clientIp, userAgent);
        return toResponse(version, server, global.getSlug());
    }

    /** 直接发布草稿版本，跳过审批流程。 */
    @Transactional
    public McpDraftResponse publishDraft(Long versionId, String operator,
                                         String clientIp, String userAgent) {
        McpVersion version = draftService.requireVersion(versionId);
        draftService.assertSubmittable(version);
        McpServer server = draftService.requireServer(version.getServerId());
        Namespace global = requireGlobalNamespace();

        version.publish();
        draftService.save(version);

        server.setLatestVersionId(version.getId());
        server.setUpdatedBy(operator);
        serverRepository.save(server);

        auditService.record(operator, "MCP_PUBLISH", versionId,
                auditService.detail("slug", server.getSlug(), "revisionNo", version.getRevisionNo()),
                clientIp, userAgent);
        return toResponse(version, server, global.getSlug());
    }

    private McpSourceType parseSourceType(String sourceType) {
        if (sourceType == null || sourceType.isBlank()) {
            throw new DomainBadRequestException("mcp.source_type.required");
        }
        try {
            return McpSourceType.valueOf(sourceType.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new DomainBadRequestException("mcp.source_type.unsupported", sourceType);
        }
    }

    private void applyMetadata(McpVersion version, McpDraftRequest request) {
        version.setUpstreamVersion(request.upstreamVersion());
        version.setPublisherName(request.publisherName());
        version.setPublisherUrl(request.publisherUrl());
        version.setHomepageUrl(request.homepageUrl());
        version.setRepositoryUrl(request.repositoryUrl());
        version.setDocumentationUrl(request.documentationUrl());
        version.setLicenseSpdx(request.licenseSpdx());
        version.setLicenseName(request.licenseName());
        version.setLicenseUrl(request.licenseUrl());
        version.setLicenseNote(request.licenseNote());
    }

    private McpDraftService.DraftMetadata toMetadata(McpDraftRequest request) {
        return new McpDraftService.DraftMetadata(
                request.displayName(), request.summary(), request.upstreamVersion(),
                request.publisherName(), request.publisherUrl(), request.homepageUrl(),
                request.repositoryUrl(), request.documentationUrl(),
                request.licenseSpdx(), request.licenseName(), request.licenseUrl(), request.licenseNote());
    }

    private void replaceCategories(Long versionId, List<Long> categoryIds) {
        versionCategoryRepository.deleteByVersionId(versionId);
        if (categoryIds == null) {
            return;
        }
        int order = 0;
        for (Long categoryId : categoryIds) {
            versionCategoryRepository.save(new McpVersionCategory(versionId, categoryId, order++));
        }
    }

    private void copyCategories(Long sourceVersionId, Long targetVersionId) {
        List<McpVersionCategory> sourceCategories = versionCategoryRepository.findByVersionId(sourceVersionId);
        if (sourceCategories.isEmpty()) {
            return;
        }
        for (McpVersionCategory sourceCategory : sourceCategories) {
            versionCategoryRepository.save(new McpVersionCategory(
                    targetVersionId,
                    sourceCategory.getCategoryId(),
                    sourceCategory.getSortOrder()
            ));
        }
    }

    private Namespace requireGlobalNamespace() {
        return namespaceRepository.findBySlug(GLOBAL_NAMESPACE_SLUG)
                .orElseThrow(() -> new IllegalStateException("Missing built-in global namespace"));
    }

    private String normalizeSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new DomainBadRequestException("mcp.slug.required");
        }
        String normalized = slug.trim().toLowerCase(java.util.Locale.ROOT);
        if (!normalized.matches("^[a-z0-9][a-z0-9-]{0,99}$")) {
            throw new DomainBadRequestException("mcp.slug.invalid", slug);
        }
        return normalized;
    }

    private McpDraftResponse toResponse(McpVersion version, McpServer server, String namespaceSlug) {
        List<Long> categoryIds = versionCategoryRepository.findByVersionId(version.getId()).stream()
                .map(McpVersionCategory::getCategoryId)
                .toList();
        return new McpDraftResponse(
                version.getId(), server.getId(), namespaceSlug, server.getSlug(),
                version.getRevisionNo(),
                version.getStatus().name(), version.getContentStatus().name(),
                version.getDisplayName(), version.getSummary(),
                version.getSourceType() != null ? version.getSourceType().name() : null,
                version.getSourceUrl(),
                version.getContentErrorCode(), version.getContentErrorMessage(),
                version.getUpstreamVersion(),
                version.getPublisherName(),
                version.getPublisherUrl(),
                version.getHomepageUrl(),
                version.getRepositoryUrl(),
                version.getDocumentationUrl(),
                version.getLicenseSpdx(),
                version.getLicenseName(),
                version.getLicenseUrl(),
                version.getLicenseNote(),
                categoryIds,
                version.getLockVersion(), version.getCreatedAt());
    }
}
