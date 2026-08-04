package com.iflytek.skillhub.domain.mcp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Clock;
import java.time.Instant;

@Entity
@Table(name = "mcp_submission")
public class McpSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version_id", nullable = false)
    private Long versionId;

    @Column(name = "namespace_id", nullable = false)
    private Long namespaceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private McpSubmissionStatus status = McpSubmissionStatus.PENDING;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Integer lockVersion = 0;

    @Column(name = "submitted_by", nullable = false, length = 128)
    private String submittedBy;

    @Column(name = "reviewed_by", length = 128)
    private String reviewedBy;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt = Instant.now(Clock.systemUTC());

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    protected McpSubmission() {}

    public McpSubmission(Long versionId, Long namespaceId, String submittedBy) {
        this.versionId = versionId;
        this.namespaceId = namespaceId;
        this.submittedBy = submittedBy;
    }

    public Long getId() { return id; }

    public Long getVersionId() { return versionId; }

    public Long getNamespaceId() { return namespaceId; }

    public McpSubmissionStatus getStatus() { return status; }

    public void setStatus(McpSubmissionStatus status) { this.status = status; }

    public Integer getLockVersion() { return lockVersion; }

    public String getSubmittedBy() { return submittedBy; }

    public String getReviewedBy() { return reviewedBy; }

    public void setReviewedBy(String reviewedBy) { this.reviewedBy = reviewedBy; }

    public String getReviewComment() { return reviewComment; }

    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }

    public Instant getSubmittedAt() { return submittedAt; }

    public Instant getReviewedAt() { return reviewedAt; }

    public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
}
