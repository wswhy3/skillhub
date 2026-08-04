package com.iflytek.skillhub.dto;

public record McpCategoryDto(
        Long id,
        String slug,
        String displayName,
        String description,
        Integer sortOrder
) {}
