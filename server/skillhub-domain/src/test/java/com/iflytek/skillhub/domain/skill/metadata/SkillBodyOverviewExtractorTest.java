package com.iflytek.skillhub.domain.skill.metadata;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SkillBodyOverviewExtractorTest {

    @Test
    void testExtractHeadlineAndSummaryFromChineseBody() {
        String body = """
            # PDF 文档阅读助手

            帮助 Agent 阅读和解析 PDF 文档内容。

            ## 使用方式
            """;

        SkillBodyOverviewExtractor.Overview overview = SkillBodyOverviewExtractor.extract(body);

        assertEquals("PDF 文档阅读助手", overview.headline());
        assertEquals("帮助 Agent 阅读和解析 PDF 文档内容。", overview.summary());
    }

    @Test
    void testHeadlinePrefixSymbolsStripped() {
        String body = "# **生成提交信息**\n\n为当前变更生成提交信息。";

        SkillBodyOverviewExtractor.Overview overview = SkillBodyOverviewExtractor.extract(body);

        assertEquals("生成提交信息", overview.headline());
        assertEquals("为当前变更生成提交信息。", overview.summary());
    }

    @Test
    void testHtmlCommentLinesSkipped() {
        String body = """
            <!-- generated header -->

            # 中文标题

            首段描述。
            """;

        SkillBodyOverviewExtractor.Overview overview = SkillBodyOverviewExtractor.extract(body);

        assertEquals("中文标题", overview.headline());
        assertEquals("首段描述。", overview.summary());
    }

    @Test
    void testDividerLineNotUsedAsSummary() {
        String body = "中文标题\n\n---\n\n这是描述。";

        SkillBodyOverviewExtractor.Overview overview = SkillBodyOverviewExtractor.extract(body);

        assertEquals("中文标题", overview.headline());
        assertEquals("这是描述。", overview.summary());
    }

    @Test
    void testLongLinesTruncated() {
        String headline = "标".repeat(300);
        String summary = "述".repeat(600);

        SkillBodyOverviewExtractor.Overview overview = SkillBodyOverviewExtractor.extract(headline + "\n\n" + summary);

        assertEquals(200, overview.headline().length());
        assertEquals(500, overview.summary().length());
    }

    @Test
    void testLeadingNumberTextPreserved() {
        String body = "2026 年度报告技能\n\n汇总全年数据。";

        SkillBodyOverviewExtractor.Overview overview = SkillBodyOverviewExtractor.extract(body);

        assertEquals("2026 年度报告技能", overview.headline());
        assertEquals("汇总全年数据。", overview.summary());
    }

    @Test
    void testBlankOrNullBodyReturnsNulls() {
        assertNull(SkillBodyOverviewExtractor.extract(null).headline());
        assertNull(SkillBodyOverviewExtractor.extract("   \n\n  ").headline());
        assertNull(SkillBodyOverviewExtractor.extract("").summary());
    }

    @Test
    void testHeadlineOnlyBodyHasNullSummary() {
        String body = "# 只有标题";

        SkillBodyOverviewExtractor.Overview overview = SkillBodyOverviewExtractor.extract(body);

        assertEquals("只有标题", overview.headline());
        assertNull(overview.summary());
    }
}
