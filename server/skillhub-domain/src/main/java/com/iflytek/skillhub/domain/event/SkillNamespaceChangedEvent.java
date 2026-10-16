package com.iflytek.skillhub.domain.event;

/**
 * Emitted after an administrator moves a skill from one namespace to another.
 * Consumers (e.g. the search index) must refresh derived coordinate-based data.
 */
public record SkillNamespaceChangedEvent(Long skillId,
                                         Long fromNamespaceId,
                                         Long toNamespaceId,
                                         String actorUserId) {}
