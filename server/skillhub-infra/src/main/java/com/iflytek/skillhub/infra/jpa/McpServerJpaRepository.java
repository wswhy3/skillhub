package com.iflytek.skillhub.infra.jpa;

import com.iflytek.skillhub.domain.mcp.McpServer;
import com.iflytek.skillhub.domain.mcp.McpServerRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface McpServerJpaRepository extends JpaRepository<McpServer, Long>, McpServerRepository {

    @Override
    Optional<McpServer> findByNamespaceIdAndSlug(Long namespaceId, String slug);

    @Override
    List<McpServer> findByIdIn(List<Long> ids);
}
