package com.iflytek.skillhub.service;

import com.iflytek.skillhub.domain.mcp.McpCategory;
import com.iflytek.skillhub.domain.mcp.McpCategoryRepository;
import com.iflytek.skillhub.domain.mcp.McpCategoryTranslation;
import com.iflytek.skillhub.domain.mcp.McpCategoryTranslationRepository;
import com.iflytek.skillhub.domain.namespace.Namespace;
import com.iflytek.skillhub.domain.namespace.NamespaceRepository;
import com.iflytek.skillhub.dto.McpCategoryDto;
import com.iflytek.skillhub.dto.McpServerDetailResponse;
import com.iflytek.skillhub.dto.McpServerSummaryResponse;
import com.iflytek.skillhub.dto.PageResponse;
import com.iflytek.skillhub.repository.McpCatalogQueryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class McpCatalogAppService {

    private static final int MAX_PAGE_SIZE = 48;
    private static final String GLOBAL_NAMESPACE_SLUG = "global";

    private final McpCatalogQueryRepository catalogQueryRepository;
    private final McpCategoryRepository categoryRepository;
    private final McpCategoryTranslationRepository categoryTranslationRepository;
    private final NamespaceRepository namespaceRepository;
    private final LocalizedTextResolver localizedTextResolver;

    public McpCatalogAppService(McpCatalogQueryRepository catalogQueryRepository,
                                McpCategoryRepository categoryRepository,
                                McpCategoryTranslationRepository categoryTranslationRepository,
                                NamespaceRepository namespaceRepository,
                                LocalizedTextResolver localizedTextResolver) {
        this.catalogQueryRepository = catalogQueryRepository;
        this.categoryRepository = categoryRepository;
        this.categoryTranslationRepository = categoryTranslationRepository;
        this.namespaceRepository = namespaceRepository;
        this.localizedTextResolver = localizedTextResolver;
    }

    @Transactional(readOnly = true)
    public List<McpCategoryDto> listCategories() {
        List<McpCategory> categories = categoryRepository.findByEnabledTrueOrderBySortOrderAsc();
        List<Long> ids = categories.stream().map(McpCategory::getId).toList();
        Map<Long, List<McpCategoryTranslation>> translations = categoryTranslationRepository
                .findByCategoryIdIn(ids).stream()
                .collect(Collectors.groupingBy(McpCategoryTranslation::getCategoryId));
        return categories.stream()
                .map(c -> {
                    List<McpCategoryTranslation> list = translations.getOrDefault(c.getId(), List.of());
                    String name = localizedTextResolver.resolve(list, McpCategoryTranslation::getLocale,
                            McpCategoryTranslation::getDisplayName, c.getSlug());
                    String desc = localizedTextResolver.resolve(list, McpCategoryTranslation::getLocale,
                            t -> t.getDescription() != null ? t.getDescription() : "", "");
                    return new McpCategoryDto(c.getId(), c.getSlug(), name,
                            desc.isBlank() ? null : desc, c.getSortOrder());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<McpServerSummaryResponse> search(String keyword, String categorySlug,
                                                         String namespaceSlug, String sort,
                                                         int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize);
        Long namespaceId = resolveNamespaceId(namespaceSlug);
        Page<McpServerSummaryResponse> result = catalogQueryRepository.searchPublished(
                keyword, categorySlug, namespaceId, sort, pageable, null);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public McpServerDetailResponse getDetail(String namespaceSlug, String slug) {
        Long namespaceId = resolveNamespaceId(namespaceSlug);
        if (namespaceId == null) {
            return null;
        }
        return catalogQueryRepository.getPublishedDetail(namespaceId, slug, null);
    }

    /** null namespaceSlug 默认 global；找不到返回 null。 */
    private Long resolveNamespaceId(String namespaceSlug) {
        String effective = (namespaceSlug == null || namespaceSlug.isBlank()) ? GLOBAL_NAMESPACE_SLUG : namespaceSlug;
        return namespaceRepository.findBySlug(effective).map(Namespace::getId).orElse(null);
    }
}
