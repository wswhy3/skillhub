package com.iflytek.skillhub.dto;

/** 治理动作请求（下架/隐藏需原因）。 */
public record McpGovernanceActionRequest(
        String reason
) {}
