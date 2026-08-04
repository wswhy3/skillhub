package com.iflytek.skillhub.dto;

import java.util.List;

/** 创建/编辑 MCP 草稿的请求。 */
public record McpDraftRequest(
        String slug,
        String displayName,
        String summary,
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
        List<Long> categoryIds
) {}
