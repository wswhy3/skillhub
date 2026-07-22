package com.iflytek.skillhub.domain.skill.validation;

import com.iflytek.skillhub.domain.skill.metadata.SkillMetadata;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiKeyRequirementDetectorTest {

    private final ApiKeyRequirementDetector detector = new ApiKeyRequirementDetector();

    private SkillMetadata metadata(Map<String, Object> frontmatter) {
        return new SkillMetadata("demo", "desc", "1.0.0", "body", frontmatter);
    }

    private PackageEntry entry(String path, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        return new PackageEntry(path, bytes, bytes.length, "text/plain");
    }

    @Test
    void frontmatterTrueWins() {
        assertTrue(detector.requiresApiKey(List.of(), metadata(Map.of("x-astron-requires-api-key", true))));
    }

    @Test
    void frontmatterFalseOverridesDetection() {
        List<PackageEntry> entries = List.of(entry("run.py", "import os\nos.environ['OPENAI_API_KEY']\n"));
        assertFalse(detector.requiresApiKey(entries, metadata(Map.of("x-astron-requires-api-key", false))));
    }

    @Test
    void detectsProcessEnv() {
        List<PackageEntry> entries = List.of(entry("index.ts", "const key = process.env.ANTHROPIC_API_KEY;"));
        assertTrue(detector.requiresApiKey(entries, metadata(Map.of())));
    }

    @Test
    void detectsOsEnviron() {
        List<PackageEntry> entries = List.of(entry("main.py", "token = os.getenv('HF_TOKEN')"));
        assertTrue(detector.requiresApiKey(entries, metadata(Map.of())));
    }

    @Test
    void detectsShellCredentialVar() {
        List<PackageEntry> entries = List.of(entry("run.sh", "curl -H \"Authorization: Bearer $OPENAI_API_KEY\" https://api.example.com"));
        assertTrue(detector.requiresApiKey(entries, metadata(Map.of())));
    }

    @Test
    void detectsCredentialNameInDocs() {
        List<PackageEntry> entries = List.of(entry("SKILL.md", "Set MOONSHOT_API_KEY before running this skill."));
        assertTrue(detector.requiresApiKey(entries, metadata(Map.of())));
    }

    @Test
    void plainSkillReturnsFalse() {
        List<PackageEntry> entries = List.of(
                entry("SKILL.md", "---\nname: demo\ndescription: plain\n---\nDo things."),
                entry("notes.txt", "nothing secret here")
        );
        assertFalse(detector.requiresApiKey(entries, metadata(Map.of())));
    }

    @Test
    void ignoresBinaryFiles() {
        List<PackageEntry> entries = List.of(entry("logo.png", "process.env.OPENAI_API_KEY"));
        assertFalse(detector.requiresApiKey(entries, metadata(Map.of())));
    }
}
