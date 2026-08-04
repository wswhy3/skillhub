package com.iflytek.skillhub.domain.mcp;

import java.util.List;
import java.util.Optional;

public interface McpVersionRepository {

    McpVersion save(McpVersion version);

    void delete(McpVersion version);

    Optional<McpVersion> findById(Long id);

    List<McpVersion> findByServerId(Long serverId);

    Optional<McpVersion> findByServerIdAndRevisionNo(Long serverId, Integer revisionNo);

    Optional<McpVersion> findTopByServerIdOrderByRevisionNoDesc(Long serverId);

    /** 最近一个可用于回退的已发布版本（排除指定版本）。 */
    Optional<McpVersion> findLatestPublishedExcluding(Long serverId, Long excludedVersionId);

    List<McpVersion> findByIdIn(List<Long> ids);

    void flush();
}
