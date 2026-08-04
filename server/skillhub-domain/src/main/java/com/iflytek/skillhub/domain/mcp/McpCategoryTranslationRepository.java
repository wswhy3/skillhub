package com.iflytek.skillhub.domain.mcp;

import java.util.List;
import java.util.Optional;

public interface McpCategoryTranslationRepository {

    McpCategoryTranslation save(McpCategoryTranslation translation);

    List<McpCategoryTranslation> findByCategoryId(Long categoryId);

    List<McpCategoryTranslation> findByCategoryIdIn(List<Long> categoryIds);

    Optional<McpCategoryTranslation> findByCategoryIdAndLocale(Long categoryId, String locale);

    void delete(McpCategoryTranslation translation);

    void flush();
}
