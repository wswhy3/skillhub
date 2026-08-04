package com.iflytek.skillhub.domain.mcp;

import java.util.List;
import java.util.Optional;

public interface McpCategoryRepository {

    McpCategory save(McpCategory category);

    Optional<McpCategory> findById(Long id);

    Optional<McpCategory> findBySlug(String slug);

    List<McpCategory> findAll();

    List<McpCategory> findByEnabledTrueOrderBySortOrderAsc();

    List<McpCategory> findByIdIn(List<Long> ids);

    void delete(McpCategory category);

    void flush();
}
