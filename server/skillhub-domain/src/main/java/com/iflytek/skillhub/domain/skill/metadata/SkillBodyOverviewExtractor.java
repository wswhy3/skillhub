package com.iflytek.skillhub.domain.skill.metadata;

/**
 * 从 SKILL.md 正文（frontmatter 之后的部分）提取展示标题与展示描述。
 *
 * <p>提取规则（与详情页“概览”首屏所见保持一致）：
 * <ul>
 *   <li>headline：第一个非空、非 HTML 注释的内容行；若为 Markdown 标题（{@code #} 前缀）
 *       则去掉标题符号，仅保留文本。</li>
 *   <li>summary：headline 之后第一个非空、非注释、非分隔线的内容行。</li>
 *   <li>提取不到时返回 {@code null}，由调用方决定写入 NULL 还是空串标记。</li>
 * </ul>
 *
 * <p>frontmatter 的 {@code name}/{@code description} 继续作为协议字段原样保留，
 * 本提取结果仅用于 UI 展示（卡片标题/描述、详情页大标题），不可用时展示层回退原字段。
 */
public final class SkillBodyOverviewExtractor {

    private static final int HEADLINE_MAX_LENGTH = 200;
    private static final int SUMMARY_MAX_LENGTH = 500;

    private SkillBodyOverviewExtractor() {
    }

    public record Overview(String headline, String summary) {
    }

    public static Overview extract(String body) {
        if (body == null || body.isBlank()) {
            return new Overview(null, null);
        }

        String headline = null;
        String summary = null;
        boolean headlineFound = false;

        for (String rawLine : body.split("\\R")) {
            String line = stripHtmlComment(rawLine);
            if (line == null) {
                continue;
            }
            String cleaned = cleanInlineMarks(line);
            if (cleaned.isEmpty() || isDividerLine(cleaned)) {
                continue;
            }
            if (!headlineFound) {
                headline = truncate(cleaned, HEADLINE_MAX_LENGTH);
                headlineFound = true;
                continue;
            }
            summary = truncate(cleaned, SUMMARY_MAX_LENGTH);
            break;
        }

        return new Overview(headline, summary);
    }

    /**
     * HTML 注释整行或行内片段返回移除后的文本；若整行皆为注释则返回 null（跳过该行）。
     */
    private static String stripHtmlComment(String line) {
        String trimmed = line.trim();
        if (trimmed.startsWith("<!--") && trimmed.endsWith("-->")) {
            return null;
        }
        return line.replaceAll("<!--.*?-->", "").trim();
    }

    private static String cleanInlineMarks(String line) {
        String text = line.trim();
        // 去掉 Markdown 标题前缀（#、##…）与引用前缀（>）
        text = text.replaceFirst("^#{1,6}\\s*", "");
        text = text.replaceFirst("^>\\s?", "");
        // 去掉行内加粗/斜体标记
        text = text.replace("*", "").replace("_", " ");
        // 去掉行内代码标记
        text = text.replace("`", "");
        // 去掉行内 HTML 标签（如 <br/>）
        text = text.replaceAll("<[^>]+>", "");
        // 去掉行首列表符号：无序列表（-/*/+）或有序列表（1. / 1)）；
        // 单独的普通数字（如“2026 年报告”开头）不受影响
        text = text.replaceFirst("^[-*+]\\s+", "");
        text = text.replaceFirst("^\\d+[.)]\\s+", "");
        return text.trim();
    }

    private static boolean isDividerLine(String text) {
        return text.matches("-{3,}") || text.matches("\\*{3,}") || text.matches("_{3,}");
    }

    private static String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }
}
