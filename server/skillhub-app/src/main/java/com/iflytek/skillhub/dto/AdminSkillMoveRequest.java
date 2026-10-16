package com.iflytek.skillhub.dto;

/**
 * Request body for admin skill namespace moves. The namespace is the target
 * team namespace slug, optionally prefixed with "@".
 */
public record AdminSkillMoveRequest(String namespace) {}
