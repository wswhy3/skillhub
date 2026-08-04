package com.iflytek.skillhub.bootstrap;

import com.iflytek.skillhub.domain.mcp.McpCategory;
import com.iflytek.skillhub.domain.mcp.McpCategoryRepository;
import com.iflytek.skillhub.domain.mcp.McpCategoryTranslation;
import com.iflytek.skillhub.domain.mcp.McpCategoryTranslationRepository;
import com.iflytek.skillhub.domain.mcp.McpServer;
import com.iflytek.skillhub.domain.mcp.McpServerRepository;
import com.iflytek.skillhub.domain.mcp.McpSourceType;
import com.iflytek.skillhub.domain.mcp.McpVersion;
import com.iflytek.skillhub.domain.mcp.McpVersionCategory;
import com.iflytek.skillhub.domain.mcp.McpVersionCategoryRepository;
import com.iflytek.skillhub.domain.mcp.McpVersionRepository;
import com.iflytek.skillhub.domain.mcp.McpVisibility;
import com.iflytek.skillhub.domain.namespace.Namespace;
import com.iflytek.skillhub.domain.namespace.NamespaceRepository;
import com.iflytek.skillhub.service.McpContentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 本地开发 MCP 示例数据：2 个已发布的虚构 MCP Server + 分类。
 * 仅在 local profile 下运行，幂等。
 */
@Component
@Profile("local")
public class LocalMcpDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalMcpDataInitializer.class);
    private static final String GLOBAL_NAMESPACE_SLUG = "global";
    private static final String OPERATOR = "local-admin";

    private final McpServerRepository serverRepository;
    private final McpVersionRepository versionRepository;
    private final McpCategoryRepository categoryRepository;
    private final McpCategoryTranslationRepository categoryTranslationRepository;
    private final McpVersionCategoryRepository versionCategoryRepository;
    private final NamespaceRepository namespaceRepository;
    private final McpContentService contentService;

    public LocalMcpDataInitializer(McpServerRepository serverRepository,
                                   McpVersionRepository versionRepository,
                                   McpCategoryRepository categoryRepository,
                                   McpCategoryTranslationRepository categoryTranslationRepository,
                                   McpVersionCategoryRepository versionCategoryRepository,
                                   NamespaceRepository namespaceRepository,
                                   McpContentService contentService) {
        this.serverRepository = serverRepository;
        this.versionRepository = versionRepository;
        this.categoryRepository = categoryRepository;
        this.categoryTranslationRepository = categoryTranslationRepository;
        this.versionCategoryRepository = versionCategoryRepository;
        this.namespaceRepository = namespaceRepository;
        this.contentService = contentService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Namespace global = namespaceRepository.findBySlug(GLOBAL_NAMESPACE_SLUG)
                .orElseThrow(() -> new IllegalStateException("Missing built-in global namespace"));

        McpCategory devtools = ensureCategory("devtools", 0,
                Map.of("en", "Dev Tools", "zh", "开发工具"));
        McpCategory database = ensureCategory("database", 1,
                Map.of("en", "Database", "zh", "数据库"));

        ensurePublishedServer(global, "github-mcp", "GitHub MCP",
                "Access GitHub repositories, issues and pull requests via MCP.",
                "github", "https://github.com/github/github-mcp-server", "MIT",
                List.of(devtools.getId()),
                "# GitHub MCP\n\nInteract with GitHub repositories, issues, and pull requests.\n\n## Features\n\n- List repositories\n- Read issues\n- Manage pull requests\n");

        ensurePublishedServer(global, "postgres-mcp", "PostgreSQL MCP",
                "Query and inspect PostgreSQL databases via MCP.",
                "postgres", "https://github.com/example/postgres-mcp", "Apache-2.0",
                List.of(database.getId()),
                "# PostgreSQL MCP\n\nQuery and inspect PostgreSQL databases.\n\n## Features\n\n- Run SQL queries\n- Inspect schemas\n- List tables\n");

        log.info("Local MCP sample data ready");
    }

    private McpCategory ensureCategory(String slug, int sortOrder, Map<String, String> translations) {
        McpCategory category = categoryRepository.findBySlug(slug)
                .orElseGet(() -> {
                    McpCategory c = new McpCategory(slug, OPERATOR);
                    c.setSortOrder(sortOrder);
                    return categoryRepository.save(c);
                });
        for (Map.Entry<String, String> entry : translations.entrySet()) {
            categoryTranslationRepository.findByCategoryIdAndLocale(category.getId(), entry.getKey())
                    .orElseGet(() -> categoryTranslationRepository.save(
                            new McpCategoryTranslation(category.getId(), entry.getKey(), entry.getValue())));
        }
        return category;
    }

    private void ensurePublishedServer(Namespace global, String slug, String displayName, String summary,
                                       String publisherName, String repositoryUrl, String licenseSpdx,
                                       List<Long> categoryIds, String readme) {
        McpServer server = serverRepository.findByNamespaceIdAndSlug(global.getId(), slug)
                .orElseGet(() -> serverRepository.save(
                        new McpServer(global.getId(), slug, OPERATOR, McpVisibility.PUBLIC)));
        if (server.getLatestVersionId() != null) {
            return;
        }

        McpVersion version = new McpVersion(server.getId(), 1, displayName, summary,
                McpSourceType.MANUAL, OPERATOR);
        version.setPublisherName(publisherName);
        version.setRepositoryUrl(repositoryUrl);
        version.setLicenseSpdx(licenseSpdx);
        version.setUpstreamVersion("1.0.0");
        version = versionRepository.save(version);

        McpContentService.FetchedReadme stored = contentService.storeManualReadme(
                readme.getBytes(StandardCharsets.UTF_8), global.getSlug(), slug);
        version.markContentReady(stored.objectKey(), stored.sha256(), stored.size());
        version.publish();
        version = versionRepository.save(version);

        int order = 0;
        for (Long categoryId : categoryIds) {
            versionCategoryRepository.save(new McpVersionCategory(version.getId(), categoryId, order++));
        }

        server.setLatestVersionId(version.getId());
        server.setUpdatedBy(OPERATOR);
        serverRepository.save(server);
    }
}
