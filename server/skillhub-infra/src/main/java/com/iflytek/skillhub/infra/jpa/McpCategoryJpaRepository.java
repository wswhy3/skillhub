package com.iflytek.skillhub.infra.jpa;

import com.iflytek.skillhub.domain.mcp.McpCategory;
import com.iflytek.skillhub.domain.mcp.McpCategoryRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface McpCategoryJpaRepository extends JpaRepository<McpCategory, Long>, McpCategoryRepository {

    @Override
    Optional<McpCategory> findBySlug(String slug);

    @Override
    List<McpCategory> findByEnabledTrueOrderBySortOrderAsc();

    @Override
    List<McpCategory> findByIdIn(List<Long> ids);
}
