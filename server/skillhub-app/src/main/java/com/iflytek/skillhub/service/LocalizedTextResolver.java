package com.iflytek.skillhub.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

/**
 * 通用本地化文本解析：按当前 locale、语言码、en、slug 的顺序回退。
 * 供 MCP 分类及后续任何 (locale, displayName) 结构复用。
 */
@Service
public class LocalizedTextResolver {

    /**
     * @param translations 任意本地化条目
     * @param localeExtractor 提取 locale
     * @param valueExtractor 提取显示值
     * @param fallback 最终回退值（通常是 slug）
     */
    public <T> String resolve(List<T> translations,
                              Function<T, String> localeExtractor,
                              Function<T, String> valueExtractor,
                              String fallback) {
        Map<String, String> values = translations.stream()
                .collect(java.util.stream.Collectors.toMap(
                        t -> normalizeLocale(localeExtractor.apply(t)),
                        valueExtractor,
                        (left, right) -> left
                ));
        Locale locale = LocaleContextHolder.getLocale();
        List<String> candidates = List.of(
                normalizeLocale(locale.toLanguageTag()),
                normalizeLocale(locale.getLanguage()),
                "en"
        );
        return candidates.stream()
                .map(values::get)
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .findFirst()
                .orElse(fallback);
    }

    private String normalizeLocale(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replace('_', '-').toLowerCase(Locale.ROOT);
    }
}
