package com.iflytek.skillhub.domain.mcp.service;

import com.iflytek.skillhub.domain.mcp.McpServer;
import com.iflytek.skillhub.domain.mcp.McpServerRepository;
import com.iflytek.skillhub.domain.mcp.McpSourceType;
import com.iflytek.skillhub.domain.mcp.McpSubmission;
import com.iflytek.skillhub.domain.mcp.McpSubmissionRepository;
import com.iflytek.skillhub.domain.mcp.McpSubmissionStatus;
import com.iflytek.skillhub.domain.mcp.McpVersion;
import com.iflytek.skillhub.domain.mcp.McpVersionRepository;
import com.iflytek.skillhub.domain.mcp.McpVersionStatus;
import com.iflytek.skillhub.domain.mcp.McpVisibility;
import com.iflytek.skillhub.domain.shared.exception.DomainBadRequestException;
import com.iflytek.skillhub.domain.shared.exception.DomainForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ConcurrentModificationException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class McpReviewServiceTest {

    @Mock private McpServerRepository serverRepository;
    @Mock private McpVersionRepository versionRepository;
    @Mock private McpSubmissionRepository submissionRepository;

    private McpDraftService draftService;
    private McpReviewService reviewService;

    private McpServer server;
    private McpVersion version;

    @BeforeEach
    void setUp() {
        draftService = new McpDraftService(serverRepository, versionRepository);
        reviewService = new McpReviewService(serverRepository, versionRepository, submissionRepository, draftService, true);

        server = new McpServer(1L, "my-mcp", "admin", McpVisibility.PUBLIC);
        version = new McpVersion(1L, 1, "Name", "Summary", McpSourceType.MANUAL, "admin");
        version.markContentReady("mcp/global/my-mcp/abc/README.md", "sha", 10L);
    }

    private void stubVersionFound() {
        when(versionRepository.findById(1L)).thenReturn(Optional.of(version));
    }

    private McpSubmission pendingSubmission() {
        return new McpSubmission(1L, 1L, "submitter");
    }

    @Nested
    class Submit {

        @Test
        void transitionsToPendingReview() {
            stubVersionFound();
            when(serverRepository.findById(1L)).thenReturn(Optional.of(server));
            when(submissionRepository.save(any(McpSubmission.class))).thenAnswer(inv -> inv.getArgument(0));

            reviewService.submit(1L, "submitter");

            assertThat(version.getStatus()).isEqualTo(McpVersionStatus.PENDING_REVIEW);
        }
    }

    @Nested
    class Withdraw {

        @Test
        void onlySubmitterCanWithdraw() {
            McpSubmission submission = pendingSubmission();
            when(submissionRepository.findById(1L)).thenReturn(Optional.of(submission));

            assertThatThrownBy(() -> reviewService.withdraw(1L, "other"))
                    .isInstanceOf(DomainForbiddenException.class);
        }

        @Test
        void withdrawReturnsVersionToDraft() {
            McpSubmission submission = pendingSubmission();
            version.setStatus(McpVersionStatus.PENDING_REVIEW);
            when(submissionRepository.findById(1L)).thenReturn(Optional.of(submission));
            stubVersionFound();

            reviewService.withdraw(1L, "submitter");

            assertThat(version.getStatus()).isEqualTo(McpVersionStatus.DRAFT);
        }
    }

    @Nested
    class Approve {

        @Test
        void publishesVersionAndUpdatesLatestPointer() {
            McpSubmission submission = pendingSubmission();
            version.setStatus(McpVersionStatus.PENDING_REVIEW);
            version.setLicenseSpdx("MIT");
            when(submissionRepository.findById(1L)).thenReturn(Optional.of(submission));
            when(submissionRepository.updateStatusWithVersion(eq(1L), eq(McpSubmissionStatus.PENDING),
                    eq(McpSubmissionStatus.APPROVED), anyInt(), anyString(), any())).thenReturn(1);
            stubVersionFound();
            when(serverRepository.findById(1L)).thenReturn(Optional.of(server));
            when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(serverRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            reviewService.approve(1L, "reviewer", 0, "ok");

            assertThat(version.getStatus()).isEqualTo(McpVersionStatus.PUBLISHED);
            assertThat(version.getPublishedAt()).isNotNull();
        }

        @Test
        void fourEyesRejectsSelfReview() {
            McpSubmission submission = pendingSubmission();
            when(submissionRepository.findById(1L)).thenReturn(Optional.of(submission));

            assertThatThrownBy(() -> reviewService.approve(1L, "submitter", 0, "ok"))
                    .isInstanceOf(DomainForbiddenException.class);
        }

        @Test
        void concurrentConflictThrows() {
            McpSubmission submission = pendingSubmission();
            version.setStatus(McpVersionStatus.PENDING_REVIEW);
            when(submissionRepository.findById(1L)).thenReturn(Optional.of(submission));
            when(submissionRepository.updateStatusWithVersion(anyLong(), any(), any(), anyInt(), anyString(), any()))
                    .thenReturn(0);

            assertThatThrownBy(() -> reviewService.approve(1L, "reviewer", 5, "ok"))
                    .isInstanceOf(ConcurrentModificationException.class);
        }

        @Test
        void licenseRequiredForApproval() {
            McpSubmission submission = pendingSubmission();
            version.setStatus(McpVersionStatus.PENDING_REVIEW);
            when(submissionRepository.findById(1L)).thenReturn(Optional.of(submission));
            when(submissionRepository.updateStatusWithVersion(anyLong(), any(), any(), anyInt(), anyString(), any()))
                    .thenReturn(1);
            stubVersionFound();

            assertThatThrownBy(() -> reviewService.approve(1L, "reviewer", 0, "ok"))
                    .isInstanceOf(DomainBadRequestException.class)
                    .hasMessageContaining("license");
        }
    }

    @Nested
    class Reject {

        @Test
        void rejectMarksVersionRejected() {
            McpSubmission submission = pendingSubmission();
            version.setStatus(McpVersionStatus.PENDING_REVIEW);
            when(submissionRepository.findById(1L)).thenReturn(Optional.of(submission));
            when(submissionRepository.updateStatusWithVersion(anyLong(), any(), any(), anyInt(), anyString(), any()))
                    .thenReturn(1);
            stubVersionFound();
            when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            reviewService.reject(1L, "reviewer", 0, "bad");

            assertThat(version.getStatus()).isEqualTo(McpVersionStatus.REJECTED);
        }
    }
}
