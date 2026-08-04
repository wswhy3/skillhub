package com.iflytek.skillhub.infra.jpa;

import com.iflytek.skillhub.domain.mcp.McpVersion;
import com.iflytek.skillhub.domain.mcp.McpVersionRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface McpVersionJpaRepository extends JpaRepository<McpVersion, Long>, McpVersionRepository {

    @Override
    List<McpVersion> findByServerId(Long serverId);

    @Override
    Optional<McpVersion> findByServerIdAndRevisionNo(Long serverId, Integer revisionNo);

    @Override
    Optional<McpVersion> findTopByServerIdOrderByRevisionNoDesc(Long serverId);

    @Override
    List<McpVersion> findByIdIn(List<Long> ids);

    @Override
    @Query("SELECT v FROM McpVersion v WHERE v.serverId = :serverId AND v.status = 'PUBLISHED' AND v.id <> :excludedId ORDER BY v.revisionNo DESC LIMIT 1")
    Optional<McpVersion> findLatestPublishedExcluding(@Param("serverId") Long serverId, @Param("excludedId") Long excludedId);
}
