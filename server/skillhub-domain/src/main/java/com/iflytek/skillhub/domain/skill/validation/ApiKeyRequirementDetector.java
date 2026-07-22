package com.iflytek.skillhub.domain.skill.validation;

import com.iflytek.skillhub.domain.skill.metadata.SkillMetadata;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Detects whether a skill package requires an API key (or similar credential) to run.
 * An explicit frontmatter declaration ({@code x-astron-requires-api-key}) always wins;
 * otherwise text-like files are scanned for environment-variable access patterns and
 * well-known credential variable names.
 */
@Component
public class ApiKeyRequirementDetector {

    public static final String FRONTMATTER_KEY = "x-astron-requires-api-key";

    private static final List<Pattern> ENV_ACCESS_PATTERNS = List.of(
            // JavaScript / TypeScript: process.env.OPENAI_API_KEY
            Pattern.compile("process\\.env\\.[A-Z_][A-Z0-9_]*"),
            // Python: os.environ["..."], os.environ.get, os.getenv(
            Pattern.compile("os\\.environ"),
            Pattern.compile("os\\.getenv\\s*\\("),
            // Java / Kotlin: System.getenv(
            Pattern.compile("System\\.getenv\\s*\\("),
            // C / Go / Python ctypes: getenv("NAME"
            Pattern.compile("\\bgetenv\\s*\\(\\s*[\"'][A-Z_][A-Z0-9_]*[\"']"),
            // PowerShell: $env:NAME
            Pattern.compile("\\$env:[A-Za-z_][A-Za-z0-9_]*"),
            // Shell usage of credential-like variables: $OPENAI_API_KEY / ${ANTHROPIC_TOKEN}
            Pattern.compile("\\$\\{?[A-Z][A-Z0-9_]*(API_KEY|ACCESS_KEY|SECRET|TOKEN)\\}?\\b")
    );

    // Well-known credential variable names mentioned anywhere (docs, comments, code)
    private static final Pattern CREDENTIAL_VAR_NAME = Pattern.compile(
            "\\b[A-Z][A-Z0-9_]*(API_KEY|ACCESS_KEY|SECRET_KEY|AUTH_TOKEN|ACCESS_TOKEN)\\b"
    );

    /**
     * Returns true when the package declares or appears to require an API key.
     */
    public boolean requiresApiKey(List<PackageEntry> entries, SkillMetadata metadata) {
        Boolean declared = frontmatterDeclaration(metadata);
        if (declared != null) {
            return declared;
        }
        return detectFromFiles(entries);
    }

    private Boolean frontmatterDeclaration(SkillMetadata metadata) {
        if (metadata == null || metadata.frontmatter() == null) {
            return null;
        }
        Object raw = metadata.frontmatter().get(FRONTMATTER_KEY);
        if (raw == null) {
            return null;
        }
        if (raw instanceof Boolean bool) {
            return bool;
        }
        String value = raw.toString().trim().toLowerCase(Locale.ROOT);
        if ("true".equals(value)) {
            return Boolean.TRUE;
        }
        if ("false".equals(value)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private boolean detectFromFiles(List<PackageEntry> entries) {
        for (PackageEntry entry : entries) {
            if (!isTextLike(entry.path())) {
                continue;
            }
            String content = new String(entry.content(), StandardCharsets.UTF_8);
            for (Pattern pattern : ENV_ACCESS_PATTERNS) {
                if (pattern.matcher(content).find()) {
                    return true;
                }
            }
            if (CREDENTIAL_VAR_NAME.matcher(content).find()) {
                return true;
            }
        }
        return false;
    }

    private boolean isTextLike(String path) {
        String lowerPath = path.toLowerCase(Locale.ROOT);
        return lowerPath.endsWith(".md") || lowerPath.endsWith(".txt")
                || lowerPath.endsWith(".json") || lowerPath.endsWith(".yaml") || lowerPath.endsWith(".yml")
                || lowerPath.endsWith(".js") || lowerPath.endsWith(".ts")
                || lowerPath.endsWith(".py") || lowerPath.endsWith(".sh")
                || lowerPath.endsWith(".html") || lowerPath.endsWith(".css")
                || lowerPath.endsWith(".toml") || lowerPath.endsWith(".xml") || lowerPath.endsWith(".ini")
                || lowerPath.endsWith(".cfg") || lowerPath.endsWith(".env")
                || lowerPath.endsWith(".rb") || lowerPath.endsWith(".go") || lowerPath.endsWith(".rs")
                || lowerPath.endsWith(".java") || lowerPath.endsWith(".kt") || lowerPath.endsWith(".lua")
                || lowerPath.endsWith(".r")
                || lowerPath.endsWith(".bat") || lowerPath.endsWith(".ps1")
                || lowerPath.endsWith(".zsh") || lowerPath.endsWith(".bash");
    }
}
