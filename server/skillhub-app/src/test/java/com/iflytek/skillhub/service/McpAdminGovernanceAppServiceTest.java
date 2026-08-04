package com.iflytek.skillhub.service;

import com.iflytek.skillhub.domain.mcp.McpServer;
import com.iflytek.skillhub.domain.mcp.McpServerRepository;
import com.iflytek.skillhub.domain.mcp.McpSourceType;
import com.iflytek.skillhub.domain.mcp.McpSubmission;
import com.iflytek.skillhub.domain.mcp.McpSubmissionRepository;
import com.iflytek.skillhub.domain.mcp.McpVersion;
import com.iflytek.skillhub.domain.mcp.McpVersionRepository;
import com.iflytek.skillhub.domain.mcp.McpVisibility;
import com.iflytek.skillhub.domain.mcp.service.McpDraftService;
import com.iflytek.skillhub.domain.mcp.service.McpGovernanceService;
import com.iflytek.skillhub.domain.namespace.NamespaceRepository;
import com.iflytek.skillhub.storage.ObjectStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class McpAdminGovernanceAppServiceTest {

    private final McpGovernanceService governanceService = mock(McpGovernanceService.class);
    private final McpServerRepository serverRepository = mock(McpServerRepository.class);
    private final McpVersionRepository versionRepository = mock(McpVersionRepository.class);
    private final McpSubmissionRepository submissionRepository = mock(McpSubmissionRepository.class);
    private final NamespaceRepository namespaceRepository = mock(NamespaceRepository.class);
    private final ObjectStorageService objectStorageService = mock(ObjectStorageService.class);
    private final McpAdminAuditService auditService = mock(McpAdminAuditService.class);

    private final McpDraftService draftService = new McpDraftService(serverRepository, versionRepository);

    private final McpAdminGovernanceAppService service = new McpAdminGovernanceAppService(
            governanceService,
            draftService,
            serverRepository,
            versionRepository,
            submissionRepository,
            namespaceRepository,
            objectStorageService,
            auditService
    );

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void deleteServer_removesVersionsSubmissionsAndStorage() {
        McpServer server = new McpServer(1L, "tapd", "admin", McpVisibility.PUBLIC);
        setId(server, 11L);
        server.setLatestVersionId(101L);

        McpVersion version = new McpVersion(11L, 1, "TAPD", "summary", McpSourceType.MANUAL, "admin");
        setId(version, 101L);
        version.setReadmeObjectKey("mcp/global/tapd/readme.md");
        version.setIconObjectKey("mcp/global/tapd/icon.png");

        McpSubmission submission = new McpSubmission(101L, 1L, "admin");

        when(serverRepository.findById(11L)).thenReturn(java.util.Optional.of(server));
        when(versionRepository.findByServerId(11L)).thenReturn(List.of(version));
        when(submissionRepository.findByVersionIdOrderBySubmittedAtDesc(101L)).thenReturn(List.of(submission));

        TransactionSynchronizationManager.initSynchronization();

        service.deleteServer(11L, "superadmin", "127.0.0.1", "JUnit");

        verify(submissionRepository).delete(submission);
        verify(serverRepository).save(server);
        verify(serverRepository).flush();
        verify(versionRepository).delete(version);
        verify(serverRepository).delete(server);
        verify(auditService).record(eq("superadmin"), eq("MCP_DELETE"), eq(11L), any(), eq("127.0.0.1"), eq("JUnit"));

        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }

        verify(objectStorageService).deleteObjects(List.of(
                "mcp/global/tapd/readme.md",
                "mcp/global/tapd/icon.png"
        ));
    }

    private static void setId(McpServer server, long id) {
        try {
            java.lang.reflect.Field field = McpServer.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(server, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void setId(McpVersion version, long id) {
        try {
            java.lang.reflect.Field field = McpVersion.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(version, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
