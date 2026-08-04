package com.iflytek.skillhub.dto;

import java.time.Instant;

public record McpAdminServerResponse(
        Long id,
        String namespace,
        String slug,
        String status,
        String visibility,
        boolean hidden,
        Long latestVersionId,
        Long draftVersionId,
        String draftDisplayName,
        Instant updatedAt
) {}
