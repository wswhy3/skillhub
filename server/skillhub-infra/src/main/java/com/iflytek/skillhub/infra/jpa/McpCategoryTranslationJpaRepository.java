package com.iflytek.skillhub.infra.jpa;

import com.iflytek.skillhub.domain.mcp.McpCategoryTranslation;
import com.iflytek.skillhub.domain.mcp.McpCategoryTranslationRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface McpCategoryTranslationJpaRepository extends JpaRepository<McpCategoryTranslation, Long>, McpCategoryTranslationRepository {

    @Override
    List<McpCategoryTranslation> findByCategoryId(Long categoryId);

    @Override
    List<McpCategoryTranslation> findByCategoryIdIn(List<Long> categoryIds);

    @Override
    Optional<McpCategoryTranslation> findByCategoryIdAndLocale(Long categoryId, String locale);
}
