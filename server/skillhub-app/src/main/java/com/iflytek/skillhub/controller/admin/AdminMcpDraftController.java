package com.iflytek.skillhub.controller.admin;

import com.iflytek.skillhub.auth.rbac.PlatformPrincipal;
import com.iflytek.skillhub.controller.BaseApiController;
import com.iflytek.skillhub.dto.ApiResponse;
import com.iflytek.skillhub.dto.ApiResponseFactory;
import com.iflytek.skillhub.dto.McpDraftRequest;
import com.iflytek.skillhub.dto.McpDraftResponse;
import com.iflytek.skillhub.dto.McpImportRequest;
import com.iflytek.skillhub.dto.McpManualReadmeRequest;
import com.iflytek.skillhub.domain.shared.exception.DomainNotFoundException;
import com.iflytek.skillhub.service.McpAdminDraftAppService;
import com.iflytek.skillhub.service.McpAssetAppService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MCP 运营后台：草稿创建、编辑、上游导入。
 */
@RestController
@RequestMapping("/api/v1/admin/mcp")
@PreAuthorize("hasAnyRole('SKILL_ADMIN', 'SUPER_ADMIN')")
public class AdminMcpDraftController extends BaseApiController {

    private final McpAdminDraftAppService draftAppService;
    private final McpAssetAppService assetAppService;

    public AdminMcpDraftController(McpAdminDraftAppService draftAppService,
                                   McpAssetAppService assetAppService,
                                   ApiResponseFactory responseFactory) {
        super(responseFactory);
        this.draftAppService = draftAppService;
        this.assetAppService = assetAppService;
    }

    @PostMapping("/drafts")
    public ApiResponse<McpDraftResponse> createDraft(@RequestBody McpDraftRequest request,
                                                     @AuthenticationPrincipal PlatformPrincipal principal,
                                                     HttpServletRequest httpRequest) {
        McpDraftResponse result = draftAppService.createDraft(request, principal.userId(),
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        return ok("response.success.created", result);
    }

    @PutMapping("/drafts/{versionId}")
    public ApiResponse<McpDraftResponse> updateDraft(@PathVariable Long versionId,
                                                     @RequestBody McpDraftRequest request,
                                                     @AuthenticationPrincipal PlatformPrincipal principal,
                                                     HttpServletRequest httpRequest) {
        McpDraftResponse result = draftAppService.updateDraft(versionId, request, principal.userId(),
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        return ok("response.success.updated", result);
    }

    @GetMapping("/drafts/{versionId}")
    public ApiResponse<McpDraftResponse> getDraft(@PathVariable Long versionId) {
        return ok("response.success.read", draftAppService.getDraft(versionId));
    }

    @GetMapping("/drafts/{versionId}/readme")
    public ResponseEntity<InputStreamResource> getDraftReadme(@PathVariable Long versionId) {
        McpAssetAppService.AssetContent asset = assetAppService.loadDraftReadme(versionId);
        if (asset == null) {
            throw new DomainNotFoundException("mcp.version.not_found", versionId);
        }
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/markdown; charset=UTF-8"));
        if (asset.size() != null) {
            builder.contentLength(asset.size());
        }
        return builder.body(new InputStreamResource(asset.stream()));
    }

    @PostMapping("/versions/{versionId}/draft")
    public ApiResponse<McpDraftResponse> createRevisionDraft(@PathVariable Long versionId,
                                                             @AuthenticationPrincipal PlatformPrincipal principal,
                                                             HttpServletRequest httpRequest) {
        McpDraftResponse result = draftAppService.createRevisionDraft(versionId, principal.userId(),
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        return ok("response.success.created", result);
    }

    @PostMapping("/drafts/{versionId}/publish")
    public ApiResponse<McpDraftResponse> publishDraft(@PathVariable Long versionId,
                                                       @AuthenticationPrincipal PlatformPrincipal principal,
                                                       HttpServletRequest httpRequest) {
        McpDraftResponse result = draftAppService.publishDraft(versionId, principal.userId(),
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        return ok("response.success.published", result);
    }

    @PostMapping("/drafts/{versionId}/import")
    public ApiResponse<McpDraftResponse> importDraft(@PathVariable Long versionId,
                                                     @RequestBody McpImportRequest request,
                                                     @AuthenticationPrincipal PlatformPrincipal principal,
                                                     HttpServletRequest httpRequest) {
        McpDraftResponse result = draftAppService.importDraft(versionId, request, principal.userId(),
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        return ok("response.success.updated", result);
    }

    @PostMapping("/drafts/{versionId}/manual-readme")
    public ApiResponse<McpDraftResponse> saveManualReadme(@PathVariable Long versionId,
                                                          @RequestBody McpManualReadmeRequest request,
                                                          @AuthenticationPrincipal PlatformPrincipal principal,
                                                          HttpServletRequest httpRequest) {
        McpDraftResponse result = draftAppService.saveManualReadme(versionId, request, principal.userId(),
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        return ok("response.success.updated", result);
    }
}
