package com.iflytek.skillhub.infra.jpa;

import com.iflytek.skillhub.domain.mcp.McpSubmission;
import com.iflytek.skillhub.domain.mcp.McpSubmissionRepository;
import com.iflytek.skillhub.domain.mcp.McpSubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface McpSubmissionJpaRepository extends JpaRepository<McpSubmission, Long>, McpSubmissionRepository {

    @Override
    @Query("SELECT s FROM McpSubmission s WHERE s.versionId = :versionId AND s.status = 'PENDING'")
    Optional<McpSubmission> findPendingByVersionId(@Param("versionId") Long versionId);

    @Override
    List<McpSubmission> findByVersionIdOrderBySubmittedAtDesc(Long versionId);

    @Override
    Page<McpSubmission> findByStatus(McpSubmissionStatus status, Pageable pageable);

    @Override
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE McpSubmission s SET s.status = :toStatus, s.reviewedBy = :reviewedBy, "
            + "s.reviewComment = :reviewComment, s.reviewedAt = CURRENT_TIMESTAMP, s.lockVersion = s.lockVersion + 1 "
            + "WHERE s.id = :id AND s.status = :fromStatus AND s.lockVersion = :expectedVersion")
    int updateStatusWithVersion(@Param("id") Long id,
                                @Param("fromStatus") McpSubmissionStatus fromStatus,
                                @Param("toStatus") McpSubmissionStatus toStatus,
                                @Param("expectedVersion") Integer expectedVersion,
                                @Param("reviewedBy") String reviewedBy,
                                @Param("reviewComment") String reviewComment);
}
