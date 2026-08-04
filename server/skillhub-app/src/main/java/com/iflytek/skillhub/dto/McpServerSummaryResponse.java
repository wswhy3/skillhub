package com.iflytek.skillhub.dto;

import java.time.Instant;
import java.util.List;

public record McpServerSummaryResponse(
        Long id,
        String namespace,
        String slug,
        String coordinate,
        String displayName,
        String summary,
        String publisherName,
        List<McpCategoryDto> categories,
        String iconUrl,
        String upstreamVersion,
        Instant updatedAt
) {}
