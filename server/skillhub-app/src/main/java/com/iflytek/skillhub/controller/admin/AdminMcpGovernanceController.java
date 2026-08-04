package com.iflytek.skillhub.controller.admin;

import com.iflytek.skillhub.auth.rbac.PlatformPrincipal;
import com.iflytek.skillhub.controller.BaseApiController;
import com.iflytek.skillhub.dto.ApiResponse;
import com.iflytek.skillhub.dto.ApiResponseFactory;
import com.iflytek.skillhub.dto.McpGovernanceActionRequest;
import com.iflytek.skillhub.service.McpAdminGovernanceAppService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * MCP 运营后台：治理动作（下架、隐藏、归档）。
 */
@RestController
@RequestMapping("/api/v1/admin/mcp")
@PreAuthorize("hasAnyRole('SKILL_ADMIN', 'SUPER_ADMIN')")
public class AdminMcpGovernanceController extends BaseApiController {

    private final McpAdminGovernanceAppService governanceAppService;

    public AdminMcpGovernanceController(McpAdminGovernanceAppService governanceAppService,
                                        ApiResponseFactory responseFactory) {
        super(responseFactory);
        this.governanceAppService = governanceAppService;
    }

    @PostMapping("/versions/{versionId}/yank")
    public ApiResponse<Void> yank(@PathVariable Long versionId,
                                  @RequestBody(required = false) McpGovernanceActionRequest request,
                                  @AuthenticationPrincipal PlatformPrincipal principal,
                                  HttpServletRequest httpRequest) {
        governanceAppService.yank(versionId, request != null ? request.reason() : null,
                principal.userId(), httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        return ok("response.success.updated", null);
    }

    @PostMapping("/{serverId}/hide")
    public ApiResponse<Void> hide(@PathVariable Long serverId,
                                  @RequestBody(required = false) McpGovernanceActionRequest request,
                                  @AuthenticationPrincipal PlatformPrincipal principal,
                                  HttpServletRequest httpRequest) {
        governanceAppService.hide(serverId, request != null ? request.reason() : null,
                principal.userId(), httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        return ok("response.success.updated", null);
    }

    @PostMapping("/{serverId}/unhide")
    public ApiResponse<Void> unhide(@PathVariable Long serverId,
                                    @AuthenticationPrincipal PlatformPrincipal principal,
                                    HttpServletRequest httpRequest) {
        governanceAppService.unhide(serverId, principal.userId(),
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        return ok("response.success.updated", null);
    }

    @PostMapping("/{serverId}/archive")
    public ApiResponse<Void> archive(@PathVariable Long serverId,
                                     @AuthenticationPrincipal PlatformPrincipal principal,
                                     HttpServletRequest httpRequest) {
        governanceAppService.archive(serverId, principal.userId(),
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        return ok("response.success.updated", null);
    }

    @PostMapping("/{serverId}/restore")
    public ApiResponse<Void> restore(@PathVariable Long serverId,
                                     @AuthenticationPrincipal PlatformPrincipal principal,
                                     HttpServletRequest httpRequest) {
        governanceAppService.restore(serverId, principal.userId(),
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        return ok("response.success.updated", null);
    }

    @DeleteMapping("/{serverId}")
    public ApiResponse<Void> delete(@PathVariable Long serverId,
                                    @AuthenticationPrincipal PlatformPrincipal principal,
                                    HttpServletRequest httpRequest) {
        governanceAppService.deleteServer(serverId, principal.userId(),
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        return ok("response.success.deleted", null);
    }
}
