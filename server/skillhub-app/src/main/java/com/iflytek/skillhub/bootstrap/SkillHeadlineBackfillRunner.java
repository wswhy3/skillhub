package com.iflytek.skillhub.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iflytek.skillhub.domain.skill.Skill;
import com.iflytek.skillhub.domain.skill.SkillRepository;
import com.iflytek.skillhub.domain.skill.SkillVersion;
import com.iflytek.skillhub.domain.skill.SkillVersionRepository;
import com.iflytek.skillhub.domain.skill.metadata.SkillBodyOverviewExtractor;
import com.iflytek.skillhub.domain.skill.metadata.SkillMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 存量技能卡面标题回填：为 headline 为 NULL（发布于本功能上线前）的技能，
 * 依据其最新版本的 SKILL.md 元数据重新提取展示标题/描述。
 *
 * <p>幂等：只扫描 headline IS NULL 的记录；提取不到（或无版本）时写空串标记已处理，
 * 避免每次启动重复解析。回填失败仅记录告警，不影响启动。
 * 可通过 {@code skillhub.skill-headline-backfill.enabled=false} 关闭。
 */
@Component
public class SkillHeadlineBackfillRunner {

    private static final Logger log = LoggerFactory.getLogger(SkillHeadlineBackfillRunner.class);

    private final SkillHeadlineBackfillProperties properties;
    private final SkillRepository skillRepository;
    private final SkillVersionRepository skillVersionRepository;
    private final ObjectMapper objectMapper;

    public SkillHeadlineBackfillRunner(SkillHeadlineBackfillProperties properties,
                                       SkillRepository skillRepository,
                                       SkillVersionRepository skillVersionRepository,
                                       ObjectMapper objectMapper) {
        this.properties = properties;
        this.skillRepository = skillRepository;
        this.skillVersionRepository = skillVersionRepository;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Async("skillhubEventExecutor")
    public void backfillAfterApplicationReady() {
        backfill();
    }

    void backfill() {
        if (!properties.isEnabled()) {
            log.info("Skill headline backfill is disabled");
            return;
        }
        List<Skill> pendingSkills = skillRepository.findByHeadlineIsNull();
        if (pendingSkills.isEmpty()) {
            return;
        }

        int updated = 0;
        for (Skill skill : pendingSkills) {
            try {
                SkillBodyOverviewExtractor.Overview overview = extractOverview(skill);
                // 提取不到时写空串：NULL 仅保留给"从未处理"的记录
                skill.setHeadline(overview.headline() != null ? overview.headline() : "");
                skill.setHeadlineSummary(overview.summary() != null ? overview.summary() : "");
                skillRepository.save(skill);
                updated++;
            } catch (Exception e) {
                log.warn("Failed to backfill headline for skill id={} slug={}: {}",
                        skill.getId(), skill.getSlug(), e.getMessage());
            }
        }
        log.info("Skill headline backfill finished: pending={}, updated={}", pendingSkills.size(), updated);
    }

    private SkillBodyOverviewExtractor.Overview extractOverview(Skill skill) throws Exception {
        if (skill.getLatestVersionId() == null) {
            return new SkillBodyOverviewExtractor.Overview(null, null);
        }
        String metadataJson = skillVersionRepository.findById(skill.getLatestVersionId())
                .map(SkillVersion::getParsedMetadataJson)
                .orElse(null);
        if (metadataJson == null || metadataJson.isBlank()) {
            return new SkillBodyOverviewExtractor.Overview(null, null);
        }
        // 发布链路将完整 SkillMetadata（含 body）序列化存入 parsed_metadata_json
        SkillMetadata metadata = objectMapper.readValue(metadataJson, SkillMetadata.class);
        return SkillBodyOverviewExtractor.extract(metadata.body());
    }
}
