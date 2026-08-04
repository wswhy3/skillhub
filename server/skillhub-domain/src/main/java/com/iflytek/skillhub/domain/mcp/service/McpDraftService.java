package com.iflytek.skillhub.domain.mcp.service;

import com.iflytek.skillhub.domain.mcp.McpContentStatus;
import com.iflytek.skillhub.domain.mcp.McpServer;
import com.iflytek.skillhub.domain.mcp.McpServerRepository;
import com.iflytek.skillhub.domain.mcp.McpServerStatus;
import com.iflytek.skillhub.domain.mcp.McpSourceType;
import com.iflytek.skillhub.domain.mcp.McpVersion;
import com.iflytek.skillhub.domain.mcp.McpVersionRepository;
import com.iflytek.skillhub.domain.mcp.McpVersionStatus;
import com.iflytek.skillhub.domain.shared.exception.DomainBadRequestException;
import com.iflytek.skillhub.domain.shared.exception.DomainNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * MCP 草稿生命周期：创建、编辑、内容就绪状态流转。
 * 状态规则见 docs 14-skill-lifecycle 对齐的 MCP 独立生命周期。
 */
@Service
public class McpDraftService {

    private final McpServerRepository serverRepository;
    private final McpVersionRepository versionRepository;

    public McpDraftService(McpServerRepository serverRepository,
                           McpVersionRepository versionRepository) {
        this.serverRepository = serverRepository;
        this.versionRepository = versionRepository;
    }

    @Transactional
    public McpVersion createDraft(McpServer server, String displayName, String summary,
                                  McpSourceType sourceType, String operator) {
        if (server.getStatus() != McpServerStatus.ACTIVE) {
            throw new DomainBadRequestException("mcp.server.not_active", server.getId());
        }
        validateMetadata(displayName, summary);
        int nextRevision = versionRepository.findTopByServerIdOrderByRevisionNoDesc(server.getId())
                .map(v -> v.getRevisionNo() + 1)
                .orElse(1);
        McpVersion version = new McpVersion(server.getId(), nextRevision, displayName, summary, sourceType, operator);
        return versionRepository.save(version);
    }

    @Transactional
    public McpVersion updateDraft(Long versionId, String operator, DraftMetadata metadata) {
        McpVersion version = requireVersion(versionId);
        if (!version.isEditable()) {
            throw new DomainBadRequestException("mcp.version.not_editable", versionId);
        }
        validateMetadata(metadata.displayName(), metadata.summary());
        version.setDisplayName(metadata.displayName());
        version.setSummary(metadata.summary());
        version.setUpstreamVersion(metadata.upstreamVersion());
        version.setPublisherName(metadata.publisherName());
        version.setPublisherUrl(metadata.publisherUrl());
        version.setHomepageUrl(metadata.homepageUrl());
        version.setRepositoryUrl(metadata.repositoryUrl());
        version.setDocumentationUrl(metadata.documentationUrl());
        version.setLicenseSpdx(metadata.licenseSpdx());
        version.setLicenseName(metadata.licenseName());
        version.setLicenseUrl(metadata.licenseUrl());
        version.setLicenseNote(metadata.licenseNote());
        return versionRepository.save(version);
    }

    @Transactional
    public McpVersion copyAsNewDraft(Long versionId, String operator) {
        McpVersion source = requireVersion(versionId);
        McpServer server = requireServer(source.getServerId());
        McpVersion existingDraft = versionRepository.findByServerId(server.getId()).stream()
                .filter(McpVersion::isEditable)
                .max(java.util.Comparator.comparing(McpVersion::getRevisionNo))
                .orElse(null);
        if (existingDraft != null) {
            return existingDraft;
        }
        McpVersion draft = createDraft(server, source.getDisplayName(), source.getSummary(),
                source.getSourceType(), operator);
        draft.setUpstreamVersion(source.getUpstreamVersion());
        draft.setPublisherName(source.getPublisherName());
        draft.setPublisherUrl(source.getPublisherUrl());
        draft.setHomepageUrl(source.getHomepageUrl());
        draft.setRepositoryUrl(source.getRepositoryUrl());
        draft.setDocumentationUrl(source.getDocumentationUrl());
        draft.setLicenseSpdx(source.getLicenseSpdx());
        draft.setLicenseName(source.getLicenseName());
        draft.setLicenseUrl(source.getLicenseUrl());
        draft.setLicenseNote(source.getLicenseNote());
        draft.setSourceUrl(source.getSourceUrl());
        draft.setSourceRevision(source.getSourceRevision());
        draft.setSourceFetchedAt(source.getSourceFetchedAt());
        draft.setSourceSha256(source.getSourceSha256());
        draft.setReadmeObjectKey(source.getReadmeObjectKey());
        draft.setReadmeSha256(source.getReadmeSha256());
        draft.setReadmeSize(source.getReadmeSize());
        draft.setIconObjectKey(source.getIconObjectKey());
        draft.setIconSha256(source.getIconSha256());
        draft.setIconContentType(source.getIconContentType());
        draft.setIconSize(source.getIconSize());
        draft.setContentStatus(source.getContentStatus());
        if (source.getContentStatus() == McpContentStatus.FAILED) {
            draft.markContentFailed(source.getContentErrorCode(), source.getContentErrorMessage());
        }
        return versionRepository.save(draft);
    }

    @Transactional
    public McpVersion markContentReady(Long versionId, String readmeObjectKey, String readmeSha256, Long readmeSize) {
        McpVersion version = requireVersion(versionId);
        if (!version.isEditable()) {
            throw new DomainBadRequestException("mcp.version.not_editable", versionId);
        }
        version.markContentReady(readmeObjectKey, readmeSha256, readmeSize);
        return versionRepository.save(version);
    }

    @Transactional
    public McpVersion markContentFailed(Long versionId, String errorCode, String errorMessage) {
        McpVersion version = requireVersion(versionId);
        version.markContentFailed(errorCode, errorMessage);
        return versionRepository.save(version);
    }

    @Transactional
    public McpVersion updateIcon(Long versionId, String iconObjectKey, String iconSha256,
                                 String iconContentType, Long iconSize) {
        McpVersion version = requireVersion(versionId);
        if (!version.isEditable()) {
            throw new DomainBadRequestException("mcp.version.not_editable", versionId);
        }
        version.setIconObjectKey(iconObjectKey);
        version.setIconSha256(iconSha256);
        version.setIconContentType(iconContentType);
        version.setIconSize(iconSize);
        return versionRepository.save(version);
    }

    public McpVersion requireVersion(Long versionId) {
        return versionRepository.findById(versionId)
                .orElseThrow(() -> new DomainNotFoundException("mcp.version.not_found", versionId));
    }

    /** 保存已由领域方法就地修改的版本（导入/内容状态流转后落库）。 */
    @Transactional
    public McpVersion save(McpVersion version) {
        return versionRepository.save(version);
    }

    public McpServer requireServer(Long serverId) {
        return serverRepository.findById(serverId)
                .orElseThrow(() -> new DomainNotFoundException("mcp.server.not_found", serverId));
    }

    /** 草稿提交审核前校验：内容必须 READY。 */
    public void assertSubmittable(McpVersion version) {
        if (version.getStatus() != McpVersionStatus.DRAFT) {
            throw new DomainBadRequestException("mcp.version.not_draft", version.getId());
        }
        if (version.getContentStatus() != McpContentStatus.READY) {
            throw new DomainBadRequestException("mcp.version.content_not_ready", version.getId());
        }
    }

    private void validateMetadata(String displayName, String summary) {
        if (displayName == null || displayName.isBlank() || displayName.length() > 200) {
            throw new DomainBadRequestException("mcp.display_name.invalid");
        }
        if (summary == null || summary.isBlank() || summary.length() > 2000) {
            throw new DomainBadRequestException("mcp.summary.invalid");
        }
    }

    public record DraftMetadata(String displayName, String summary, String upstreamVersion,
                                String publisherName, String publisherUrl, String homepageUrl,
                                String repositoryUrl, String documentationUrl,
                                String licenseSpdx, String licenseName, String licenseUrl,
                                String licenseNote) {}
}
