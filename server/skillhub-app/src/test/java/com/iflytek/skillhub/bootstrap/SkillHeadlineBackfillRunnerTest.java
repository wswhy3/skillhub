package com.iflytek.skillhub.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.skillhub.domain.skill.Skill;
import com.iflytek.skillhub.domain.skill.SkillRepository;
import com.iflytek.skillhub.domain.skill.SkillVersion;
import com.iflytek.skillhub.domain.skill.SkillVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillHeadlineBackfillRunnerTest {

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private SkillVersionRepository skillVersionRepository;

    private SkillHeadlineBackfillProperties properties;
    private SkillHeadlineBackfillRunner runner;

    @BeforeEach
    void setUp() {
        properties = new SkillHeadlineBackfillProperties();
        runner = new SkillHeadlineBackfillRunner(
                properties, skillRepository, skillVersionRepository, new ObjectMapper());
    }

    @Test
    void backfillShouldExtractHeadlineFromStoredMetadata() {
        Skill skill = mock(Skill.class);
        when(skill.getLatestVersionId()).thenReturn(5L);
        when(skillRepository.findByHeadlineIsNull()).thenReturn(List.of(skill));

        SkillVersion version = mock(SkillVersion.class);
        // 发布链路存的是完整 SkillMetadata（含 body）的 JSON
        when(version.getParsedMetadataJson()).thenReturn(
                "{\"name\":\"demo\",\"description\":\"English summary\",\"version\":\"1.0.0\","
                        + "\"body\":\"# 中文演示技能\\n\\n中文描述行。\"}");
        when(skillVersionRepository.findById(5L)).thenReturn(Optional.of(version));

        runner.backfill();

        verify(skill).setHeadline("中文演示技能");
        verify(skill).setHeadlineSummary("中文描述行。");
        verify(skillRepository).save(skill);
    }

    @Test
    void backfillShouldWriteEmptyStringWhenNothingExtractable() {
        Skill skill = mock(Skill.class);
        when(skill.getLatestVersionId()).thenReturn(null);
        when(skillRepository.findByHeadlineIsNull()).thenReturn(List.of(skill));

        runner.backfill();

        // 空串标记已处理，避免下次启动重复扫描
        verify(skill).setHeadline("");
        verify(skill).setHeadlineSummary("");
        verify(skillRepository).save(skill);
    }

    @Test
    void backfillShouldBeNoOpWhenDisabled() {
        properties.setEnabled(false);

        runner.backfill();

        verify(skillRepository, never()).findByHeadlineIsNull();
        verify(skillRepository, never()).save(any());
    }

    @Test
    void backfillShouldSkipSkillsWithoutPendingRecords() {
        when(skillRepository.findByHeadlineIsNull()).thenReturn(List.of());

        runner.backfill();

        verify(skillRepository, never()).save(any());
    }
}
