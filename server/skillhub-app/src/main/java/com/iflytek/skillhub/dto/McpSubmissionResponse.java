package com.iflytek.skillhub.dto;

import java.time.Instant;

public record McpSubmissionResponse(
        Long id,
        Long versionId,
        Long serverId,
        String namespace,
        String slug,
        String displayName,
        String status,
        String submittedBy,
        String reviewedBy,
        String reviewComment,
        Integer lockVersion,
        Instant submittedAt,
        Instant reviewedAt
) {}
