package com.iflytek.skillhub.domain.mcp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Clock;
import java.time.Instant;

/**
 * MCP 版本的不可变发布快照。DRAFT 阶段允许更新，提交审核后内容冻结。
 * 对象的存储 key 只在内部流转，不进入公共响应。
 */
@Entity
@Table(name = "mcp_version")
public class McpVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "server_id", nullable = false)
    private Long serverId;

    @Column(name = "revision_no", nullable = false)
    private Integer revisionNo;

    @Column(name = "upstream_version", length = 128)
    private String upstreamVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private McpVersionStatus status = McpVersionStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_status", nullable = false, length = 32)
    private McpContentStatus contentStatus = McpContentStatus.PENDING;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "publisher_name", length = 200)
    private String publisherName;

    @Column(name = "publisher_url", length = 1024)
    private String publisherUrl;

    @Column(name = "homepage_url", length = 1024)
    private String homepageUrl;

    @Column(name = "repository_url", length = 1024)
    private String repositoryUrl;

    @Column(name = "documentation_url", length = 1024)
    private String documentationUrl;

    @Column(name = "license_spdx", length = 64)
    private String licenseSpdx;

    @Column(name = "license_name", length = 128)
    private String licenseName;

    @Column(name = "license_url", length = 1024)
    private String licenseUrl;

    @Column(name = "license_note", columnDefinition = "TEXT")
    private String licenseNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32)
    private McpSourceType sourceType = McpSourceType.MANUAL;

    @Column(name = "source_url", length = 2048)
    private String sourceUrl;

    @Column(name = "source_revision", length = 256)
    private String sourceRevision;

    @Column(name = "source_fetched_at")
    private Instant sourceFetchedAt;

    @Column(name = "source_sha256", length = 64)
    private String sourceSha256;

    @Column(name = "readme_object_key", length = 512)
    private String readmeObjectKey;

    @Column(name = "readme_sha256", length = 64)
    private String readmeSha256;

    @Column(name = "readme_size")
    private Long readmeSize;

    @Column(name = "icon_object_key", length = 512)
    private String iconObjectKey;

    @Column(name = "icon_sha256", length = 64)
    private String iconSha256;

    @Column(name = "icon_content_type", length = 128)
    private String iconContentType;

    @Column(name = "icon_size")
    private Long iconSize;

    @Column(name = "content_error_code", length = 64)
    private String contentErrorCode;

    @Column(name = "content_error_message", length = 512)
    private String contentErrorMessage;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Integer lockVersion = 0;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "yanked_at")
    private Instant yankedAt;

    @Column(name = "yanked_by", length = 128)
    private String yankedBy;

    @Column(name = "yank_reason", columnDefinition = "TEXT")
    private String yankReason;

    @Column(name = "created_by", nullable = false, length = 128)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected McpVersion() {}

    public McpVersion(Long serverId, Integer revisionNo, String displayName, String summary,
                      McpSourceType sourceType, String createdBy) {
        this.serverId = serverId;
        this.revisionNo = revisionNo;
        this.displayName = displayName;
        this.summary = summary;
        this.sourceType = sourceType;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now(Clock.systemUTC());
    }

    public boolean isEditable() {
        return this.status == McpVersionStatus.DRAFT;
    }

    public void markContentReady(String readmeObjectKey, String readmeSha256, Long readmeSize) {
        this.readmeObjectKey = readmeObjectKey;
        this.readmeSha256 = readmeSha256;
        this.readmeSize = readmeSize;
        this.contentStatus = McpContentStatus.READY;
        this.contentErrorCode = null;
        this.contentErrorMessage = null;
    }

    public void markContentFailed(String errorCode, String errorMessage) {
        this.contentStatus = McpContentStatus.FAILED;
        this.contentErrorCode = errorCode;
        this.contentErrorMessage = errorMessage;
    }

    public void publish() {
        this.status = McpVersionStatus.PUBLISHED;
        this.publishedAt = Instant.now(Clock.systemUTC());
    }

    public void reject() {
        this.status = McpVersionStatus.REJECTED;
    }

    public void yank(String operator, String reason) {
        this.status = McpVersionStatus.YANKED;
        this.yankedAt = Instant.now(Clock.systemUTC());
        this.yankedBy = operator;
        this.yankReason = reason;
    }

    public Long getId() { return id; }

    public Long getServerId() { return serverId; }

    public Integer getRevisionNo() { return revisionNo; }

    public String getUpstreamVersion() { return upstreamVersion; }

    public void setUpstreamVersion(String upstreamVersion) { this.upstreamVersion = upstreamVersion; }

    public McpVersionStatus getStatus() { return status; }

    public void setStatus(McpVersionStatus status) { this.status = status; }

    public McpContentStatus getContentStatus() { return contentStatus; }

    public void setContentStatus(McpContentStatus contentStatus) { this.contentStatus = contentStatus; }

    public String getDisplayName() { return displayName; }

    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getSummary() { return summary; }

    public void setSummary(String summary) { this.summary = summary; }

    public String getPublisherName() { return publisherName; }

    public void setPublisherName(String publisherName) { this.publisherName = publisherName; }

    public String getPublisherUrl() { return publisherUrl; }

    public void setPublisherUrl(String publisherUrl) { this.publisherUrl = publisherUrl; }

    public String getHomepageUrl() { return homepageUrl; }

    public void setHomepageUrl(String homepageUrl) { this.homepageUrl = homepageUrl; }

    public String getRepositoryUrl() { return repositoryUrl; }

    public void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; }

    public String getDocumentationUrl() { return documentationUrl; }

    public void setDocumentationUrl(String documentationUrl) { this.documentationUrl = documentationUrl; }

    public String getLicenseSpdx() { return licenseSpdx; }

    public void setLicenseSpdx(String licenseSpdx) { this.licenseSpdx = licenseSpdx; }

    public String getLicenseName() { return licenseName; }

    public void setLicenseName(String licenseName) { this.licenseName = licenseName; }

    public String getLicenseUrl() { return licenseUrl; }

    public void setLicenseUrl(String licenseUrl) { this.licenseUrl = licenseUrl; }

    public String getLicenseNote() { return licenseNote; }

    public void setLicenseNote(String licenseNote) { this.licenseNote = licenseNote; }

    public McpSourceType getSourceType() { return sourceType; }

    public void setSourceType(McpSourceType sourceType) { this.sourceType = sourceType; }

    public String getSourceUrl() { return sourceUrl; }

    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

    public String getSourceRevision() { return sourceRevision; }

    public void setSourceRevision(String sourceRevision) { this.sourceRevision = sourceRevision; }

    public Instant getSourceFetchedAt() { return sourceFetchedAt; }

    public void setSourceFetchedAt(Instant sourceFetchedAt) { this.sourceFetchedAt = sourceFetchedAt; }

    public String getSourceSha256() { return sourceSha256; }

    public void setSourceSha256(String sourceSha256) { this.sourceSha256 = sourceSha256; }

    public String getReadmeObjectKey() { return readmeObjectKey; }

    public void setReadmeObjectKey(String readmeObjectKey) { this.readmeObjectKey = readmeObjectKey; }

    public String getReadmeSha256() { return readmeSha256; }

    public void setReadmeSha256(String readmeSha256) { this.readmeSha256 = readmeSha256; }

    public Long getReadmeSize() { return readmeSize; }

    public void setReadmeSize(Long readmeSize) { this.readmeSize = readmeSize; }

    public String getIconObjectKey() { return iconObjectKey; }

    public void setIconObjectKey(String iconObjectKey) { this.iconObjectKey = iconObjectKey; }

    public String getIconSha256() { return iconSha256; }

    public void setIconSha256(String iconSha256) { this.iconSha256 = iconSha256; }

    public String getIconContentType() { return iconContentType; }

    public void setIconContentType(String iconContentType) { this.iconContentType = iconContentType; }

    public Long getIconSize() { return iconSize; }

    public void setIconSize(Long iconSize) { this.iconSize = iconSize; }

    public String getContentErrorCode() { return contentErrorCode; }

    public String getContentErrorMessage() { return contentErrorMessage; }

    public Integer getLockVersion() { return lockVersion; }

    public Instant getPublishedAt() { return publishedAt; }

    public Instant getYankedAt() { return yankedAt; }

    public String getYankedBy() { return yankedBy; }

    public String getYankReason() { return yankReason; }

    public String getCreatedBy() { return createdBy; }

    public Instant getCreatedAt() { return createdAt; }
}
