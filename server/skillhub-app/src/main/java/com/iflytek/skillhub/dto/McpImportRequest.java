package com.iflytek.skillhub.dto;

/** 通过上游来源导入 MCP 元数据的请求。 */
public record McpImportRequest(
        String sourceType,
        String sourceUrl,
        String sourceRevision
) {}
