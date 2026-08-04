package com.iflytek.skillhub.service;

import com.iflytek.skillhub.domain.mcp.McpServer;
import com.iflytek.skillhub.domain.mcp.McpSubmission;
import com.iflytek.skillhub.domain.mcp.McpSubmissionRepository;
import com.iflytek.skillhub.domain.mcp.McpSubmissionStatus;
import com.iflytek.skillhub.domain.mcp.McpVersion;
import com.iflytek.skillhub.domain.mcp.service.McpDraftService;
import com.iflytek.skillhub.domain.mcp.service.McpReviewService;
import com.iflytek.skillhub.domain.namespace.Namespace;
import com.iflytek.skillhub.domain.namespace.NamespaceRepository;
import com.iflytek.skillhub.dto.McpSubmissionResponse;
import com.iflytek.skillhub.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * MCP 审核编排：提交、撤回、通过、拒绝、队列查询。
 */
@Service
public class McpAdminReviewAppService {

    private final McpReviewService reviewService;
    private final McpDraftService draftService;
    private final McpSubmissionRepository submissionRepository;
    private final NamespaceRepository namespaceRepository;
    private final McpAdminAuditService auditService;

    public McpAdminReviewAppService(McpReviewService reviewService,
                                    McpDraftService draftService,
                                    McpSubmissionRepository submissionRepository,
                                    NamespaceRepository namespaceRepository,
                                    McpAdminAuditService auditService) {
        this.reviewService = reviewService;
        this.draftService = draftService;
        this.submissionRepository = submissionRepository;
        this.namespaceRepository = namespaceRepository;
        this.auditService = auditService;
    }

    @Transactional
    public McpSubmissionResponse submit(Long versionId, String operator, String clientIp, String userAgent) {
        McpSubmission submission = reviewService.submit(versionId, operator);
        auditService.record(operator, "MCP_SUBMIT", submission.getId(),
                auditService.detail("versionId", versionId), clientIp, userAgent);
        return toResponse(submission);
    }

    @Transactional
    public void withdraw(Long submissionId, String operator, String clientIp, String userAgent) {
        reviewService.withdraw(submissionId, operator);
        auditService.record(operator, "MCP_WITHDRAW", submissionId, Map.of(), clientIp, userAgent);
    }

    @Transactional
    public McpSubmissionResponse approve(Long submissionId, Integer lockVersion, String comment,
                                         String reviewer, String clientIp, String userAgent) {
        McpSubmission submission = reviewService.approve(submissionId, reviewer, lockVersion, comment);
        auditService.record(reviewer, "MCP_APPROVE", submissionId,
                auditService.detail("versionId", submission.getVersionId()), clientIp, userAgent);
        return toResponse(submission);
    }

    @Transactional
    public McpSubmissionResponse reject(Long submissionId, Integer lockVersion, String comment,
                                        String reviewer, String clientIp, String userAgent) {
        McpSubmission submission = reviewService.reject(submissionId, reviewer, lockVersion, comment);
        auditService.record(reviewer, "MCP_REJECT", submissionId,
                auditService.detail("versionId", submission.getVersionId(), "comment", comment),
                clientIp, userAgent);
        return toResponse(submission);
    }

    @Transactional(readOnly = true)
    public PageResponse<McpSubmissionResponse> listSubmissions(String status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "submittedAt"));
        Page<McpSubmission> result = (status == null || status.isBlank())
                ? submissionRepository.findAll(pageable)
                : submissionRepository.findByStatus(parseStatus(status), pageable);
        return PageResponse.from(result.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public McpSubmissionResponse getSubmission(Long submissionId) {
        return toResponse(reviewService.requireSubmission(submissionId));
    }

    private McpSubmissionStatus parseStatus(String status) {
        try {
            return McpSubmissionStatus.valueOf(status.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return McpSubmissionStatus.PENDING;
        }
    }

    private McpSubmissionResponse toResponse(McpSubmission submission) {
        McpVersion version = draftService.requireVersion(submission.getVersionId());
        McpServer server = draftService.requireServer(version.getServerId());
        String namespaceSlug = namespaceRepository.findById(server.getNamespaceId())
                .map(Namespace::getSlug).orElse("global");
        return new McpSubmissionResponse(
                submission.getId(), version.getId(), server.getId(), namespaceSlug, server.getSlug(),
                version.getDisplayName(), submission.getStatus().name(),
                submission.getSubmittedBy(), submission.getReviewedBy(), submission.getReviewComment(),
                submission.getLockVersion(), submission.getSubmittedAt(), submission.getReviewedAt());
    }
}
