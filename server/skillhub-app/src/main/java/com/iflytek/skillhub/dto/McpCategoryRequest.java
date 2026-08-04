package com.iflytek.skillhub.dto;

import java.util.Map;

public record McpCategoryRequest(
        String slug,
        Integer sortOrder,
        Boolean enabled,
        Map<String, String> translations
) {}
