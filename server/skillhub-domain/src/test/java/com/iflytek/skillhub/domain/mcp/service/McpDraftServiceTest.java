package com.iflytek.skillhub.domain.mcp.service;

import com.iflytek.skillhub.domain.mcp.McpContentStatus;
import com.iflytek.skillhub.domain.mcp.McpServer;
import com.iflytek.skillhub.domain.mcp.McpServerRepository;
import com.iflytek.skillhub.domain.mcp.McpSourceType;
import com.iflytek.skillhub.domain.mcp.McpVersion;
import com.iflytek.skillhub.domain.mcp.McpVersionRepository;
import com.iflytek.skillhub.domain.mcp.McpVersionStatus;
import com.iflytek.skillhub.domain.mcp.McpVisibility;
import com.iflytek.skillhub.domain.shared.exception.DomainBadRequestException;
import com.iflytek.skillhub.domain.shared.exception.DomainNotFoundException;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class McpDraftServiceTest {

    @Mock private McpServerRepository serverRepository;
    @Mock private McpVersionRepository versionRepository;

    private McpDraftService draftService;

    @BeforeEach
    void setUp() {
        draftService = new McpDraftService(serverRepository, versionRepository);
    }

    private McpServer newServer() {
        return new McpServer(1L, "my-mcp", "admin", McpVisibility.PUBLIC);
    }

    @Nested
    class CreateDraft {

        @Test
        void createsFirstRevisionAsDraft() {
            McpServer server = newServer();
            when(versionRepository.findTopByServerIdOrderByRevisionNoDesc(null)).thenReturn(Optional.empty());
            when(versionRepository.save(any(McpVersion.class))).thenAnswer(inv -> inv.getArgument(0));

            McpVersion draft = draftService.createDraft(server, "My MCP", "A test MCP server",
                    McpSourceType.MANUAL, "admin");

            assertThat(draft.getRevisionNo()).isEqualTo(1);
            assertThat(draft.getStatus()).isEqualTo(McpVersionStatus.DRAFT);
            assertThat(draft.getContentStatus()).isEqualTo(McpContentStatus.PENDING);
            assertThat(draft.getCreatedBy()).isEqualTo("admin");
        }

        @Test
        void rejectsBlankDisplayName() {
            McpServer server = newServer();
            assertThatThrownBy(() -> draftService.createDraft(server, " ", "summary",
                    McpSourceType.MANUAL, "admin"))
                    .isInstanceOf(DomainBadRequestException.class);
        }

        @Test
        void rejectsSummaryOver2000Chars() {
            McpServer server = newServer();
            String longSummary = "x".repeat(2001);
            assertThatThrownBy(() -> draftService.createDraft(server, "Name", longSummary,
                    McpSourceType.MANUAL, "admin"))
                    .isInstanceOf(DomainBadRequestException.class);
        }
    }

    @Nested
    class UpdateDraft {

        @Test
        void rejectsUpdateWhenNotDraft() {
            McpVersion version = new McpVersion(1L, 1, "Name", "Summary", McpSourceType.MANUAL, "admin");
            version.setStatus(McpVersionStatus.PENDING_REVIEW);
            when(versionRepository.findById(1L)).thenReturn(Optional.of(version));

            McpDraftService.DraftMetadata metadata = new McpDraftService.DraftMetadata(
                    "N", "S", null, null, null, null, null, null, null, null, null, null);

            assertThatThrownBy(() -> draftService.updateDraft(1L, "admin", metadata))
                    .isInstanceOf(DomainBadRequestException.class);
        }

        @Test
        void throwsNotFoundForMissingVersion() {
            when(versionRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> draftService.requireVersion(99L))
                    .isInstanceOf(DomainNotFoundException.class);
        }
    }

    @Nested
    class AssertSubmittable {

        @Test
        void rejectsWhenContentNotReady() {
            McpVersion version = new McpVersion(1L, 1, "Name", "Summary", McpSourceType.MANUAL, "admin");
            assertThatThrownBy(() -> draftService.assertSubmittable(version))
                    .isInstanceOf(DomainBadRequestException.class)
                    .hasMessageContaining("content_not_ready");
        }

        @Test
        void acceptsReadyDraft() {
            McpVersion version = new McpVersion(1L, 1, "Name", "Summary", McpSourceType.MANUAL, "admin");
            version.markContentReady("mcp/global/x/abc/README.md", "sha", 100L);
            draftService.assertSubmittable(version);
        }
    }
}
