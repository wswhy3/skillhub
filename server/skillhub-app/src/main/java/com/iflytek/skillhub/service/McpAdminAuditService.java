package com.iflytek.skillhub.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.skillhub.domain.audit.AuditLogService;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP 运营后台审计：统一动作命名与 detail JSON 构造。
 * detail JSON 必须用 ObjectMapper 构造，不做字符串拼接。
 */
@Service
public class McpAdminAuditService {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public McpAdminAuditService(AuditLogService auditLogService, ObjectMapper objectMapper) {
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    public void record(String actor, String action, Long targetId, Map<String, Object> detail,
                       String clientIp, String userAgent) {
        auditLogService.record(actor, action, "MCP", targetId, MDC.get("requestId"),
                clientIp, userAgent, toJson(detail));
    }

    public void record(String actor, String action, String targetType, Long targetId,
                       Map<String, Object> detail, String clientIp, String userAgent) {
        auditLogService.record(actor, action, targetType, targetId, MDC.get("requestId"),
                clientIp, userAgent, toJson(detail));
    }

    public Map<String, Object> detail(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }

    private String toJson(Map<String, Object> detail) {
        if (detail == null || detail.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
