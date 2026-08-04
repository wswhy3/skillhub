package com.iflytek.skillhub.dto;

import java.time.Instant;
import java.util.List;

public record McpServerDetailResponse(
        Long id,
        String namespace,
        String slug,
        String coordinate,
        String displayName,
        String summary,
        String publisherName,
        String publisherUrl,
        String homepageUrl,
        String repositoryUrl,
        String documentationUrl,
        String licenseSpdx,
        String licenseName,
        String licenseUrl,
        String licenseNote,
        List<McpCategoryDto> categories,
        String iconUrl,
        String readmeUrl,
        String upstreamVersion,
        Integer currentRevision,
        String sourceType,
        String sourceUrl,
        String sourceRevision,
        Instant sourceFetchedAt,
        Instant publishedAt,
        Instant updatedAt
) {}
