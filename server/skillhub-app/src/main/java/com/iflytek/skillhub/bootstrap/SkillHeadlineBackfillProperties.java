package com.iflytek.skillhub.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 存量技能卡面中文标题回填开关。
 * 关闭后仅新发布/审核通过的技能会写入 headline。
 */
@ConfigurationProperties(prefix = "skillhub.skill-headline-backfill")
public class SkillHeadlineBackfillProperties {

    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
