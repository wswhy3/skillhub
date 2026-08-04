package com.iflytek.skillhub.dto;

/** 审核决定请求：乐观锁版本号 + 意见。 */
public record McpReviewActionRequest(
        Integer lockVersion,
        String comment
) {}
