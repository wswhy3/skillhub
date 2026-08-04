package com.iflytek.skillhub.domain.mcp.service;

import com.iflytek.skillhub.domain.mcp.McpServer;
import com.iflytek.skillhub.domain.mcp.McpServerRepository;
import com.iflytek.skillhub.domain.mcp.McpServerStatus;
import com.iflytek.skillhub.domain.mcp.McpSourceType;
import com.iflytek.skillhub.domain.mcp.McpVersion;
import com.iflytek.skillhub.domain.mcp.McpVersionRepository;
import com.iflytek.skillhub.domain.mcp.McpVersionStatus;
import com.iflytek.skillhub.domain.mcp.McpVisibility;
import com.iflytek.skillhub.domain.shared.exception.DomainBadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class McpGovernanceServiceTest {

    @Mock private McpServerRepository serverRepository;
    @Mock private McpVersionRepository versionRepository;

    private McpDraftService draftService;
    private McpGovernanceService governanceService;

    private McpServer server;

    @BeforeEach
    void setUp() {
        draftService = new McpDraftService(serverRepository, versionRepository);
        governanceService = new McpGovernanceService(serverRepository, versionRepository, draftService);
        server = new McpServer(1L, "my-mcp", "admin", McpVisibility.PUBLIC);
    }

    private McpVersion publishedVersion(long id, int revision) {
        McpVersion v = new McpVersion(1L, revision, "Name", "Summary", McpSourceType.MANUAL, "admin");
        v.setStatus(McpVersionStatus.PUBLISHED);
        setId(v, id);
        return v;
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

    @Nested
    class Yank {

        @Test
        void yankFallsBackToPreviousPublishedVersion() {
            McpVersion current = publishedVersion(2L, 2);
            McpVersion previous = publishedVersion(1L, 1);
            server.setLatestVersionId(2L);

            when(versionRepository.findById(2L)).thenReturn(Optional.of(current));
            when(serverRepository.findById(1L)).thenReturn(Optional.of(server));
            when(versionRepository.findLatestPublishedExcluding(isNull(), eq(2L))).thenReturn(Optional.of(previous));
            when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(serverRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            governanceService.yank(2L, "admin", "security issue");

            assertThat(current.getStatus()).isEqualTo(McpVersionStatus.YANKED);
            assertThat(server.getLatestVersionId()).isEqualTo(1L);
        }

        @Test
        void yankClearsLatestWhenNoFallback() {
            McpVersion current = publishedVersion(2L, 1);
            server.setLatestVersionId(2L);

            when(versionRepository.findById(2L)).thenReturn(Optional.of(current));
            when(serverRepository.findById(1L)).thenReturn(Optional.of(server));
            when(versionRepository.findLatestPublishedExcluding(isNull(), eq(2L))).thenReturn(Optional.empty());
            when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(serverRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            governanceService.yank(2L, "admin", "bad");

            assertThat(server.getLatestVersionId()).isNull();
        }

        @Test
        void rejectsYankOfNonPublished() {
            McpVersion draft = new McpVersion(1L, 1, "N", "S", McpSourceType.MANUAL, "admin");
            when(versionRepository.findById(3L)).thenReturn(Optional.of(draft));

            assertThatThrownBy(() -> governanceService.yank(3L, "admin", "x"))
                    .isInstanceOf(DomainBadRequestException.class);
        }
    }

    @Nested
    class HideAndArchive {

        @Test
        void hideSetsOverlayFields() {
            when(serverRepository.findById(1L)).thenReturn(Optional.of(server));
            when(serverRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            McpServer result = governanceService.hide(1L, "superadmin");

            assertThat(result.isHidden()).isTrue();
            assertThat(result.getHiddenBy()).isEqualTo("superadmin");
            assertThat(result.getHiddenAt()).isNotNull();
            assertThat(result.getStatus()).isEqualTo(McpServerStatus.ACTIVE);
        }

        @Test
        void unhideClearsOverlayFields() {
            server.hide("superadmin");
            when(serverRepository.findById(1L)).thenReturn(Optional.of(server));
            when(serverRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            McpServer result = governanceService.unhide(1L, "superadmin");

            assertThat(result.isHidden()).isFalse();
            assertThat(result.getHiddenBy()).isNull();
        }

        @Test
        void archiveAndRestore() {
            when(serverRepository.findById(1L)).thenReturn(Optional.of(server));
            when(serverRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            governanceService.archive(1L, "superadmin");
            assertThat(server.getStatus()).isEqualTo(McpServerStatus.ARCHIVED);

            governanceService.restore(1L, "superadmin");
            assertThat(server.getStatus()).isEqualTo(McpServerStatus.ACTIVE);
        }

        @Test
        void restoreRejectsNonArchived() {
            when(serverRepository.findById(1L)).thenReturn(Optional.of(server));

            assertThatThrownBy(() -> governanceService.restore(1L, "superadmin"))
                    .isInstanceOf(DomainBadRequestException.class);
        }
    }
}
