package com.iflytek.skillhub.dto;

import java.time.Instant;

public record McpDraftResponse(
        Long versionId,
        Long serverId,
        String namespace,
        String slug,
        Integer revisionNo,
        String status,
        String contentStatus,
        String displayName,
        String summary,
        String sourceType,
        String sourceUrl,
        String contentErrorCode,
        String contentErrorMessage,
        String upstreamVersion,
        String publisherName,
        String publisherUrl,
        String homepageUrl,
        String repositoryUrl,
        String documentationUrl,
        String licenseSpdx,
        String licenseName,
        String licenseUrl,
        String licenseNote,
        java.util.List<Long> categoryIds,
        Integer lockVersion,
        Instant createdAt
) {}
