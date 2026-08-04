package com.iflytek.skillhub.domain.mcp;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface McpServerRepository {

    McpServer save(McpServer server);

    void delete(McpServer server);

    Optional<McpServer> findById(Long id);

    Optional<McpServer> findByNamespaceIdAndSlug(Long namespaceId, String slug);

    List<McpServer> findByIdIn(List<Long> ids);

    Page<McpServer> findAll(Pageable pageable);

    void flush();
}
