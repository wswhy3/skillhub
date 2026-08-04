package com.iflytek.skillhub.service;

import com.iflytek.skillhub.domain.mcp.McpContentStatus;
import com.iflytek.skillhub.domain.mcp.McpServer;
import com.iflytek.skillhub.domain.mcp.McpServerRepository;
import com.iflytek.skillhub.domain.mcp.McpVersionCategory;
import com.iflytek.skillhub.domain.mcp.McpSourceType;
import com.iflytek.skillhub.domain.mcp.McpVersion;
import com.iflytek.skillhub.domain.mcp.McpVersionCategoryRepository;
import com.iflytek.skillhub.domain.mcp.McpVisibility;
import com.iflytek.skillhub.domain.mcp.service.McpDraftService;
import com.iflytek.skillhub.domain.namespace.Namespace;
import com.iflytek.skillhub.domain.namespace.NamespaceRepository;
import com.iflytek.skillhub.dto.McpDraftResponse;
import com.iflytek.skillhub.dto.McpManualReadmeRequest;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class McpAdminDraftAppServiceTest {

    private final McpDraftService draftService = mock(McpDraftService.class);
    private final McpServerRepository serverRepository = mock(McpServerRepository.class);
    private final McpVersionCategoryRepository versionCategoryRepository = mock(McpVersionCategoryRepository.class);
    private final NamespaceRepository namespaceRepository = mock(NamespaceRepository.class);
    private final McpContentService contentService = mock(McpContentService.class);
    private final McpAdminAuditService auditService = mock(McpAdminAuditService.class);

    private final McpAdminDraftAppService service = new McpAdminDraftAppService(
            draftService,
            serverRepository,
            versionCategoryRepository,
            namespaceRepository,
            contentService,
            auditService
    );

    @Test
    void createRevisionDraft_copiesCategoriesAndKeepsReadyContent() {
        Namespace global = new Namespace("global", "Global", "system");
        ReflectionTestUtils.setField(global, "id", 1L);

        McpServer server = new McpServer(1L, "mcp-server-tapd", "admin", McpVisibility.PUBLIC);
        ReflectionTestUtils.setField(server, "id", 11L);

        McpVersion draft = new McpVersion(11L, 4, "TAPD MCP Server", "summary", McpSourceType.MANUAL, "admin");
        ReflectionTestUtils.setField(draft, "id", 102L);
        ReflectionTestUtils.setField(draft, "createdAt", Instant.parse("2026-08-03T11:00:00Z"));
        draft.markContentReady("mcp/global/mcp-server-tapd/key/README.md", "sha256-ready", 256L);

        when(draftService.copyAsNewDraft(101L, "admin")).thenReturn(draft);
        when(draftService.requireServer(11L)).thenReturn(server);
        when(namespaceRepository.findBySlug("global")).thenReturn(Optional.of(global));
        when(versionCategoryRepository.findByVersionId(101L)).thenReturn(List.of(
                new McpVersionCategory(101L, 7L, 0),
                new McpVersionCategory(101L, 9L, 1)
        ));
        when(versionCategoryRepository.findByVersionId(102L)).thenReturn(
                List.of(),
                List.of(
                        new McpVersionCategory(102L, 7L, 0),
                        new McpVersionCategory(102L, 9L, 1)
                )
        );

        McpDraftResponse response = service.createRevisionDraft(
                101L,
                "admin",
                "127.0.0.1",
                "JUnit"
        );

        assertThat(response.versionId()).isEqualTo(102L);
        assertThat(response.revisionNo()).isEqualTo(4);
        assertThat(response.contentStatus()).isEqualTo("READY");
        assertThat(response.categoryIds()).containsExactly(7L, 9L);
        verify(versionCategoryRepository).save(argThat(category ->
                category.getVersionId().equals(102L)
                        && category.getCategoryId().equals(7L)
                        && category.getSortOrder().equals(0)));
        verify(versionCategoryRepository).save(argThat(category ->
                category.getVersionId().equals(102L)
                        && category.getCategoryId().equals(9L)
                        && category.getSortOrder().equals(1)));
        verify(contentService, never()).storeManualReadme(any(), any(), any());
    }

    @Test
    void saveManualReadme_marksDraftReadyAndReturnsUpdatedResponse() {
        Namespace global = new Namespace("global", "Global", "system");
        ReflectionTestUtils.setField(global, "id", 1L);

        McpServer server = new McpServer(1L, "mcp-server-tapd", "admin", McpVisibility.PUBLIC);
        ReflectionTestUtils.setField(server, "id", 11L);

        McpVersion version = new McpVersion(11L, 3, "TAPD MCP Server", "summary", McpSourceType.MANUAL, "admin");
        ReflectionTestUtils.setField(version, "id", 101L);
        ReflectionTestUtils.setField(version, "createdAt", Instant.parse("2026-08-03T10:00:00Z"));

        when(draftService.requireVersion(101L)).thenReturn(version);
        when(draftService.requireServer(11L)).thenReturn(server);
        when(namespaceRepository.findBySlug("global")).thenReturn(Optional.of(global));
        when(contentService.storeManualReadme(any(), eq("global"), eq("mcp-server-tapd")))
                .thenReturn(new McpContentService.FetchedReadme("mcp/global/mcp-server-tapd/key/README.md", "sha256", 128L, null));

        McpDraftResponse response = service.saveManualReadme(
                101L,
                new McpManualReadmeRequest("# TAPD MCP Server"),
                "admin",
                "127.0.0.1",
                "JUnit"
        );

        assertThat(response.versionId()).isEqualTo(101L);
        assertThat(response.slug()).isEqualTo("mcp-server-tapd");
        assertThat(response.contentStatus()).isEqualTo("READY");
        assertThat(version.getContentStatus()).isEqualTo(McpContentStatus.READY);
        assertThat(version.getSourceType()).isEqualTo(McpSourceType.MANUAL);
        assertThat(version.getSourceSha256()).isEqualTo("sha256");
        verify(draftService).save(version);
    }
}
