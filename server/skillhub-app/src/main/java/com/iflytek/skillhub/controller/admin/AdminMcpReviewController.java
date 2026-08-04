package com.iflytek.skillhub.controller.admin;

import com.iflytek.skillhub.auth.rbac.PlatformPrincipal;
import com.iflytek.skillhub.controller.BaseApiController;
import com.iflytek.skillhub.dto.ApiResponse;
import com.iflytek.skillhub.dto.ApiResponseFactory;
import com.iflytek.skillhub.dto.McpReviewActionRequest;
import com.iflytek.skillhub.dto.McpSubmissionResponse;
import com.iflytek.skillhub.dto.PageResponse;
import com.iflytek.skillhub.service.McpAdminReviewAppService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * MCP 运营后台：审核队列、提交、撤回、通过、拒绝。
 */
@RestController
@RequestMapping("/api/v1/admin/mcp")
@PreAuthorize("hasAnyRole('SKILL_ADMIN', 'SUPER_ADMIN')")
public class AdminMcpReviewController extends BaseApiController {

    private final McpAdminReviewAppService reviewAppService;

    public AdminMcpReviewController(McpAdminReviewAppService reviewAppService,
                                    ApiResponseFactory responseFactory) {
        super(responseFactory);
        this.reviewAppService = reviewAppService;
    }

    @GetMapping("/review/submissions")
    public ApiResponse<PageResponse<McpSubmissionResponse>> listSubmissions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return ok("response.success.read", reviewAppService.listSubmissions(status, page, size));
    }

    @GetMapping("/review/submissions/{submissionId}")
    public ApiResponse<McpSubmissionResponse> getSubmission(@PathVariable Long submissionId) {
        return ok("response.success.read", reviewAppService.getSubmission(submissionId));
    }

    @PostMapping("/drafts/{versionId}/submit")
    public ApiResponse<McpSubmissionResponse> submit(@PathVariable Long versionId,
                                                     @AuthenticationPrincipal PlatformPrincipal principal,
                                                     HttpServletRequest httpRequest) {
        McpSubmissionResponse result = reviewAppService.submit(versionId, principal.userId(),
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        return ok("response.success.updated", result);
    }

    @PostMapping("/submissions/{submissionId}/withdraw")
    public ApiResponse<Void> withdraw(@PathVariable Long submissionId,
                                      @AuthenticationPrincipal PlatformPrincipal principal,
                                      HttpServletRequest httpRequest) {
        reviewAppService.withdraw(submissionId, principal.userId(),
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        return ok("response.success.updated", null);
    }

    @PostMapping("/submissions/{submissionId}/approve")
    public ApiResponse<McpSubmissionResponse> approve(@PathVariable Long submissionId,
                                                      @RequestBody(required = false) McpReviewActionRequest request,
                                                      @AuthenticationPrincipal PlatformPrincipal principal,
                                                      HttpServletRequest httpRequest) {
        McpSubmissionResponse result = reviewAppService.approve(submissionId,
                request != null ? request.lockVersion() : 0,
                request != null ? request.comment() : null,
                principal.userId(), httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        return ok("response.success.updated", result);
    }

    @PostMapping("/submissions/{submissionId}/reject")
    public ApiResponse<McpSubmissionResponse> reject(@PathVariable Long submissionId,
                                                     @RequestBody(required = false) McpReviewActionRequest request,
                                                     @AuthenticationPrincipal PlatformPrincipal principal,
                                                     HttpServletRequest httpRequest) {
        McpSubmissionResponse result = reviewAppService.reject(submissionId,
                request != null ? request.lockVersion() : 0,
                request != null ? request.comment() : null,
                principal.userId(), httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        return ok("response.success.updated", result);
    }
}
