package com.iflytek.skillhub.domain.mcp.service;

import com.iflytek.skillhub.domain.mcp.McpServer;
import com.iflytek.skillhub.domain.mcp.McpServerRepository;
import com.iflytek.skillhub.domain.mcp.McpSubmission;
import com.iflytek.skillhub.domain.mcp.McpSubmissionRepository;
import com.iflytek.skillhub.domain.mcp.McpSubmissionStatus;
import com.iflytek.skillhub.domain.mcp.McpVersion;
import com.iflytek.skillhub.domain.mcp.McpVersionRepository;
import com.iflytek.skillhub.domain.mcp.McpVersionStatus;
import com.iflytek.skillhub.domain.shared.exception.DomainBadRequestException;
import com.iflytek.skillhub.domain.shared.exception.DomainForbiddenException;
import com.iflytek.skillhub.domain.shared.exception.DomainNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ConcurrentModificationException;
import java.util.Objects;

/**
 * MCP 审核状态机：提交、撤回、通过、拒绝。
 * 通过时原子更新 mcp_server.latest_version_id；并发冲突抛 ConcurrentModificationException。
 */
@Service
public class McpReviewService {

    private final McpServerRepository serverRepository;
    private final McpVersionRepository versionRepository;
    private final McpSubmissionRepository submissionRepository;
    private final McpDraftService draftService;
    private final boolean fourEyesEnabled;

    public McpReviewService(McpServerRepository serverRepository,
                            McpVersionRepository versionRepository,
                            McpSubmissionRepository submissionRepository,
                            McpDraftService draftService,
                            @org.springframework.beans.factory.annotation.Value("${skillhub.mcp.review.four-eyes-enabled:false}") boolean fourEyesEnabled) {
        this.serverRepository = serverRepository;
        this.versionRepository = versionRepository;
        this.submissionRepository = submissionRepository;
        this.draftService = draftService;
        this.fourEyesEnabled = fourEyesEnabled;
    }

    @Transactional
    public McpSubmission submit(Long versionId, String operator) {
        McpVersion version = draftService.requireVersion(versionId);
        draftService.assertSubmittable(version);
        McpServer server = draftService.requireServer(version.getServerId());

        version.setStatus(McpVersionStatus.PENDING_REVIEW);
        versionRepository.save(version);

        McpSubmission submission = new McpSubmission(versionId, server.getNamespaceId(), operator);
        try {
            return submissionRepository.save(submission);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new DomainBadRequestException("mcp.submit.duplicate", versionId);
        }
    }

    @Transactional
    public void withdraw(Long submissionId, String operator) {
        McpSubmission submission = requireSubmission(submissionId);
        if (submission.getStatus() != McpSubmissionStatus.PENDING) {
            throw new DomainBadRequestException("mcp.submission.not_pending", submissionId);
        }
        if (!Objects.equals(submission.getSubmittedBy(), operator)) {
            throw new DomainForbiddenException("mcp.submission.withdraw_forbidden");
        }
        McpVersion version = draftService.requireVersion(submission.getVersionId());
        version.setStatus(McpVersionStatus.DRAFT);
        versionRepository.save(version);
        submissionRepository.delete(submission);
    }

    @Transactional
    public McpSubmission approve(Long submissionId, String reviewer, Integer lockVersion, String comment) {
        McpSubmission submission = requireSubmission(submissionId);
        assertPending(submission);
        assertFourEyes(submission, reviewer, comment);

        int updated = submissionRepository.updateStatusWithVersion(
                submissionId, McpSubmissionStatus.PENDING, McpSubmissionStatus.APPROVED,
                lockVersion, reviewer, comment);
        if (updated == 0) {
            throw new ConcurrentModificationException("mcp.submission.concurrent");
        }

        McpVersion version = draftService.requireVersion(submission.getVersionId());
        if (version.getStatus() != McpVersionStatus.PENDING_REVIEW) {
            throw new DomainBadRequestException("mcp.version.not_pending_review", version.getId());
        }
        assertLicensePresent(version);

        version.publish();
        versionRepository.save(version);

        McpServer server = draftService.requireServer(version.getServerId());
        server.setLatestVersionId(version.getId());
        server.setUpdatedBy(reviewer);
        serverRepository.save(server);

        // updateStatusWithVersion 使用 clearAutomatically，原实体已脱离持久化上下文；
        // 重新加载以返回带最新 lockVersion/reviewedAt 的状态。
        return requireSubmission(submissionId);
    }

    @Transactional
    public McpSubmission reject(Long submissionId, String reviewer, Integer lockVersion, String comment) {
        McpSubmission submission = requireSubmission(submissionId);
        assertPending(submission);
        assertFourEyes(submission, reviewer, comment);

        int updated = submissionRepository.updateStatusWithVersion(
                submissionId, McpSubmissionStatus.PENDING, McpSubmissionStatus.REJECTED,
                lockVersion, reviewer, comment);
        if (updated == 0) {
            throw new ConcurrentModificationException("mcp.submission.concurrent");
        }

        McpVersion version = draftService.requireVersion(submission.getVersionId());
        version.reject();
        versionRepository.save(version);

        return requireSubmission(submissionId);
    }

    public McpSubmission requireSubmission(Long submissionId) {
        return submissionRepository.findById(submissionId)
                .orElseThrow(() -> new DomainNotFoundException("mcp.submission.not_found", submissionId));
    }

    private void assertPending(McpSubmission submission) {
        if (submission.getStatus() != McpSubmissionStatus.PENDING) {
            throw new DomainBadRequestException("mcp.submission.not_pending", submission.getId());
        }
    }

    /**
     * 四眼原则：开启后提交人不能审核自己的版本。
     * 生产建议 MCP_REVIEW_FOUR_EYES_ENABLED=true。
     */
    private void assertFourEyes(McpSubmission submission, String reviewer, String comment) {
        if (fourEyesEnabled && Objects.equals(submission.getSubmittedBy(), reviewer)) {
            throw new DomainForbiddenException("mcp.review.four_eyes");
        }
    }

    /** 发布前必须填写可识别 SPDX，或填写名称/链接/内部依据。 */
    private void assertLicensePresent(McpVersion version) {
        boolean hasSpdx = version.getLicenseSpdx() != null && !version.getLicenseSpdx().isBlank();
        boolean hasManualLicense = (version.getLicenseName() != null && !version.getLicenseName().isBlank())
                || (version.getLicenseUrl() != null && !version.getLicenseUrl().isBlank())
                || (version.getLicenseNote() != null && !version.getLicenseNote().isBlank());
        if (!hasSpdx && !hasManualLicense) {
            throw new DomainBadRequestException("mcp.license.required", version.getId());
        }
    }
}
