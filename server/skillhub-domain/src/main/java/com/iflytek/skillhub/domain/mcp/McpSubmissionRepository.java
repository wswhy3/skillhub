package com.iflytek.skillhub.domain.mcp;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface McpSubmissionRepository {

    McpSubmission save(McpSubmission submission);

    Optional<McpSubmission> findById(Long id);

    Optional<McpSubmission> findPendingByVersionId(Long versionId);

    List<McpSubmission> findByVersionIdOrderBySubmittedAtDesc(Long versionId);

    Page<McpSubmission> findByStatus(McpSubmissionStatus status, Pageable pageable);

    Page<McpSubmission> findAll(Pageable pageable);

    void delete(McpSubmission submission);

    /**
     * 乐观锁状态更新，返回受影响行数；0 表示版本冲突。
     */
    int updateStatusWithVersion(Long id, McpSubmissionStatus fromStatus, McpSubmissionStatus toStatus,
                                Integer expectedVersion, String reviewedBy, String reviewComment);

    void flush();
}
