package com.iflytek.skillhub.repository;

import com.iflytek.skillhub.domain.mcp.McpServer;
import com.iflytek.skillhub.dto.McpServerDetailResponse;
import com.iflytek.skillhub.dto.McpServerSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * MCP 广场读模型装配：列表/详情需要跨 mcp_server / mcp_version / namespace / category
 * 多表投影，因此独立查询仓库，不扩张领域仓储。
 */
public interface McpCatalogQueryRepository {

    /**
     * 公共列表：仅返回容器 ACTIVE、未隐藏、PUBLIC 且有发布版本的条目。
     * 排序必须以 (updatedAt DESC, id DESC) 稳定收口。
     */
    Page<McpServerSummaryResponse> searchPublished(String keyword, String categorySlug,
                                                   Long namespaceId, String sort, Pageable pageable,
                                                   String locale);

    /** 公共详情：返回 null 表示不可见或不存在。 */
    McpServerDetailResponse getPublishedDetail(Long namespaceId, String slug, String locale);
}
