package com.iflytek.skillhub.domain.mcp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Clock;
import java.time.Instant;

/**
 * MCP Server 聚合根与稳定坐标。
 * hidden 为治理覆盖字段，不属于生命周期状态。
 */
@Entity
@Table(name = "mcp_server")
public class McpServer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "namespace_id", nullable = false)
    private Long namespaceId;

    @Column(nullable = false, length = 100)
    private String slug;

    @Column(name = "owner_id", nullable = false, length = 128)
    private String ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private McpVisibility visibility = McpVisibility.PUBLIC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private McpServerStatus status = McpServerStatus.ACTIVE;

    @Column(name = "latest_version_id")
    private Long latestVersionId;

    @Column(nullable = false)
    private boolean hidden = false;

    @Column(name = "hidden_at")
    private Instant hiddenAt;

    @Column(name = "hidden_by", length = 128)
    private String hiddenBy;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Integer lockVersion = 0;

    @Column(name = "created_by", nullable = false, length = 128)
    private String createdBy;

    @Column(name = "updated_by", length = 128)
    private String updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected McpServer() {}

    public McpServer(Long namespaceId, String slug, String ownerId, McpVisibility visibility) {
        this.namespaceId = namespaceId;
        this.slug = slug;
        this.ownerId = ownerId;
        this.visibility = visibility;
        this.createdBy = ownerId;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now(Clock.systemUTC());
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now(Clock.systemUTC());
    }

    public void hide(String operator) {
        this.hidden = true;
        this.hiddenAt = Instant.now(Clock.systemUTC());
        this.hiddenBy = operator;
    }

    public void unhide() {
        this.hidden = false;
        this.hiddenAt = null;
        this.hiddenBy = null;
    }

    public void archive() {
        this.status = McpServerStatus.ARCHIVED;
    }

    public void restore() {
        this.status = McpServerStatus.ACTIVE;
    }

    public Long getId() { return id; }

    public Long getNamespaceId() { return namespaceId; }

    public String getSlug() { return slug; }

    public void setSlug(String slug) { this.slug = slug; }

    public String getOwnerId() { return ownerId; }

    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public McpVisibility getVisibility() { return visibility; }

    public void setVisibility(McpVisibility visibility) { this.visibility = visibility; }

    public McpServerStatus getStatus() { return status; }

    public Long getLatestVersionId() { return latestVersionId; }

    public void setLatestVersionId(Long latestVersionId) { this.latestVersionId = latestVersionId; }

    public boolean isHidden() { return hidden; }

    public Instant getHiddenAt() { return hiddenAt; }

    public String getHiddenBy() { return hiddenBy; }

    public Integer getLockVersion() { return lockVersion; }

    public String getCreatedBy() { return createdBy; }

    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }

    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
}
