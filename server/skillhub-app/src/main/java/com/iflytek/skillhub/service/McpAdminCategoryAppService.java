package com.iflytek.skillhub.service;

import com.iflytek.skillhub.domain.mcp.McpCategory;
import com.iflytek.skillhub.domain.mcp.McpCategoryRepository;
import com.iflytek.skillhub.domain.mcp.McpCategoryTranslation;
import com.iflytek.skillhub.domain.mcp.McpCategoryTranslationRepository;
import com.iflytek.skillhub.domain.shared.exception.DomainBadRequestException;
import com.iflytek.skillhub.domain.shared.exception.DomainNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * MCP 分类管理。
 */
@Service
public class McpAdminCategoryAppService {

    private final McpCategoryRepository categoryRepository;
    private final McpCategoryTranslationRepository translationRepository;
    private final McpAdminAuditService auditService;

    public McpAdminCategoryAppService(McpCategoryRepository categoryRepository,
                                      McpCategoryTranslationRepository translationRepository,
                                      McpAdminAuditService auditService) {
        this.categoryRepository = categoryRepository;
        this.translationRepository = translationRepository;
        this.auditService = auditService;
    }

    @Transactional
    public McpCategory createCategory(String slug, Integer sortOrder, Map<String, String> translations,
                                      String operator, String clientIp, String userAgent) {
        McpCategory category = new McpCategory(normalizeSlug(slug), operator);
        if (sortOrder != null) {
            category.setSortOrder(sortOrder);
        }
        try {
            category = categoryRepository.save(category);
        } catch (DataIntegrityViolationException e) {
            throw new DomainBadRequestException("mcp.category.duplicate", slug);
        }
        saveTranslations(category.getId(), translations);
        auditService.record(operator, "MCP_CATEGORY_CREATE", category.getId(),
                auditService.detail("slug", category.getSlug()), clientIp, userAgent);
        return category;
    }

    @Transactional
    public McpCategory updateCategory(Long categoryId, Integer sortOrder, Boolean enabled,
                                      Map<String, String> translations, String operator,
                                      String clientIp, String userAgent) {
        McpCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new DomainNotFoundException("mcp.category.not_found", categoryId));
        if (sortOrder != null) {
            category.setSortOrder(sortOrder);
        }
        if (enabled != null) {
            category.setEnabled(enabled);
        }
        category = categoryRepository.save(category);
        if (translations != null) {
            saveTranslations(categoryId, translations);
        }
        auditService.record(operator, "MCP_CATEGORY_UPDATE", categoryId,
                auditService.detail("slug", category.getSlug()), clientIp, userAgent);
        return category;
    }

    @Transactional(readOnly = true)
    public List<McpCategory> listAll() {
        return categoryRepository.findAll();
    }

    private void saveTranslations(Long categoryId, Map<String, String> translations) {
        if (translations == null) {
            return;
        }
        for (Map.Entry<String, String> entry : translations.entrySet()) {
            String locale = entry.getKey();
            String displayName = entry.getValue();
            if (displayName == null || displayName.isBlank()) {
                continue;
            }
            McpCategoryTranslation translation = translationRepository
                    .findByCategoryIdAndLocale(categoryId, locale)
                    .orElseGet(() -> new McpCategoryTranslation(categoryId, locale, displayName));
            translation.setDisplayName(displayName);
            translationRepository.save(translation);
        }
    }

    private String normalizeSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new DomainBadRequestException("mcp.category.slug_required");
        }
        String normalized = slug.trim().toLowerCase(java.util.Locale.ROOT);
        if (!normalized.matches("^[a-z0-9][a-z0-9-]{0,99}$")) {
            throw new DomainBadRequestException("mcp.category.slug_invalid", slug);
        }
        return normalized;
    }
}
