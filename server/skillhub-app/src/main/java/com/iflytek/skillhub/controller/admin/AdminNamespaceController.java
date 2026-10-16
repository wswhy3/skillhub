package com.iflytek.skillhub.controller.admin;

import com.iflytek.skillhub.controller.BaseApiController;
import com.iflytek.skillhub.dto.ApiResponse;
import com.iflytek.skillhub.dto.ApiResponseFactory;
import com.iflytek.skillhub.dto.NamespaceResponse;
import com.iflytek.skillhub.service.NamespacePortalQueryAppService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administrative namespace endpoints. Backs admin workflows (such as moving
 * skills between namespaces) that need the full namespace catalog rather than
 * the caller's membership-scoped view.
 */
@RestController
@RequestMapping("/api/v1/admin/namespaces")
public class AdminNamespaceController extends BaseApiController {

    private final NamespacePortalQueryAppService namespacePortalQueryAppService;

    public AdminNamespaceController(ApiResponseFactory responseFactory,
                                    NamespacePortalQueryAppService namespacePortalQueryAppService) {
        super(responseFactory);
        this.namespacePortalQueryAppService = namespacePortalQueryAppService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SKILL_ADMIN', 'SUPER_ADMIN')")
    public ApiResponse<List<NamespaceResponse>> listNamespaces() {
        return ok("response.success.read", namespacePortalQueryAppService.listAllActiveNamespaces());
    }
}
