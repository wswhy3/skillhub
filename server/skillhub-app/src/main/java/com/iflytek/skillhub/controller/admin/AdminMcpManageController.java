package com.iflytek.skillhub.controller.admin;

import com.iflytek.skillhub.auth.rbac.PlatformPrincipal;
import com.iflytek.skillhub.controller.BaseApiController;
import com.iflytek.skillhub.domain.mcp.McpCategory;
import com.iflytek.skillhub.dto.ApiResponse;
import com.iflytek.skillhub.dto.ApiResponseFactory;
import com.iflytek.skillhub.dto.McpAdminServerResponse;
import com.iflytek.skillhub.dto.McpCategoryRequest;
import com.iflytek.skillhub.dto.PageResponse;
import com.iflytek.skillhub.service.McpAdminCategoryAppService;
import com.iflytek.skillhub.service.McpAdminGovernanceAppService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * MCP 运营后台：管理列表（按状态/关键字）+ 分类管理。
 */
@RestController
@RequestMapping("/api/v1/admin/mcp")
@PreAuthorize("hasAnyRole('SKILL_ADMIN', 'SUPER_ADMIN')")
public class AdminMcpManageController extends BaseApiController {

    private final McpAdminGovernanceAppService governanceAppService;
    private final McpAdminCategoryAppService categoryAppService;

    public AdminMcpManageController(McpAdminGovernanceAppService governanceAppService,
                                    McpAdminCategoryAppService categoryAppService,
                                    ApiResponseFactory responseFactory) {
        super(responseFactory);
        this.governanceAppService = governanceAppService;
        this.categoryAppService = categoryAppService;
    }

    @GetMapping("/servers")
    public ApiResponse<PageResponse<McpAdminServerResponse>> listServers(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        return ok("response.success.read", governanceAppService.listServers(page, size));
    }

    @GetMapping("/categories")
    public ApiResponse<List<McpCategory>> listCategories() {
        return ok("response.success.read", categoryAppService.listAll());
    }

    @PostMapping("/categories")
    public ApiResponse<McpCategory> createCategory(@RequestBody McpCategoryRequest request,
                                                   @AuthenticationPrincipal PlatformPrincipal principal,
                                                   HttpServletRequest httpRequest) {
        McpCategory category = categoryAppService.createCategory(request.slug(), request.sortOrder(),
                request.translations(), principal.userId(),
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        return ok("response.success.created", category);
    }

    @PutMapping("/categories/{categoryId}")
    public ApiResponse<McpCategory> updateCategory(@PathVariable Long categoryId,
                                                   @RequestBody McpCategoryRequest request,
                                                   @AuthenticationPrincipal PlatformPrincipal principal,
                                                   HttpServletRequest httpRequest) {
        McpCategory category = categoryAppService.updateCategory(categoryId, request.sortOrder(),
                request.enabled(), request.translations(), principal.userId(),
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        return ok("response.success.updated", category);
    }
}
