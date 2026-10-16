package com.iflytek.skillhub.dto;

/**
 * Response for admin skill namespace moves; carries the skill's new coordinate.
 */
public record AdminSkillMoveResponse(
    Long skillId,
    String namespace,
    String slug,
    String status
) {}
