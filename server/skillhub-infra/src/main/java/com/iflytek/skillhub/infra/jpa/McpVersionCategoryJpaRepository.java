package com.iflytek.skillhub.infra.jpa;

import com.iflytek.skillhub.domain.mcp.McpVersionCategory;
import com.iflytek.skillhub.domain.mcp.McpVersionCategoryId;
import com.iflytek.skillhub.domain.mcp.McpVersionCategoryRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface McpVersionCategoryJpaRepository extends JpaRepository<McpVersionCategory, McpVersionCategoryId>, McpVersionCategoryRepository {

    @Override
    List<McpVersionCategory> findByVersionId(Long versionId);

    @Override
    List<McpVersionCategory> findByVersionIdIn(List<Long> versionIds);

    @Override
    @Modifying
    @Transactional
    @Query("DELETE FROM McpVersionCategory vc WHERE vc.versionId = :versionId")
    void deleteByVersionId(@Param("versionId") Long versionId);
}
