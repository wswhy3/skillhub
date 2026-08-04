package com.iflytek.skillhub.domain.mcp.service;

import com.iflytek.skillhub.domain.mcp.McpServer;
import com.iflytek.skillhub.domain.mcp.McpServerRepository;
import com.iflytek.skillhub.domain.mcp.McpServerStatus;
import com.iflytek.skillhub.domain.mcp.McpVersion;
import com.iflytek.skillhub.domain.mcp.McpVersionRepository;
import com.iflytek.skillhub.domain.mcp.McpVersionStatus;
import com.iflytek.skillhub.domain.shared.exception.DomainBadRequestException;
import com.iflytek.skillhub.domain.shared.exception.DomainNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * MCP 治理：下架、隐藏、归档。
 * 下架当前版本时事务内回退到最近一个可用已发布版本，无可用版本则置空。
 */
@Service
public class McpGovernanceService {

    private final McpServerRepository serverRepository;
    private final McpVersionRepository versionRepository;
    private final McpDraftService draftService;

    public McpGovernanceService(McpServerRepository serverRepository,
                                McpVersionRepository versionRepository,
                                McpDraftService draftService) {
        this.serverRepository = serverRepository;
        this.versionRepository = versionRepository;
        this.draftService = draftService;
    }

    @Transactional
    public McpVersion yank(Long versionId, String operator, String reason) {
        McpVersion version = draftService.requireVersion(versionId);
        if (version.getStatus() != McpVersionStatus.PUBLISHED) {
            throw new DomainBadRequestException("mcp.version.not_published", versionId);
        }
        version.yank(operator, reason);
        versionRepository.save(version);

        McpServer server = draftService.requireServer(version.getServerId());
        if (Objects.equals(server.getLatestVersionId(), versionId)) {
            Long fallback = versionRepository.findLatestPublishedExcluding(server.getId(), versionId)
                    .map(McpVersion::getId)
                    .orElse(null);
            server.setLatestVersionId(fallback);
        }
        server.setUpdatedBy(operator);
        serverRepository.save(server);
        return version;
    }

    @Transactional
    public McpServer hide(Long serverId, String operator) {
        McpServer server = draftService.requireServer(serverId);
        server.hide(operator);
        server.setUpdatedBy(operator);
        return serverRepository.save(server);
    }

    @Transactional
    public McpServer unhide(Long serverId, String operator) {
        McpServer server = draftService.requireServer(serverId);
        server.unhide();
        server.setUpdatedBy(operator);
        return serverRepository.save(server);
    }

    @Transactional
    public McpServer archive(Long serverId, String operator) {
        McpServer server = draftService.requireServer(serverId);
        if (server.getStatus() != McpServerStatus.ACTIVE) {
            throw new DomainBadRequestException("mcp.server.not_active", serverId);
        }
        server.archive();
        server.setUpdatedBy(operator);
        return serverRepository.save(server);
    }

    @Transactional
    public McpServer restore(Long serverId, String operator) {
        McpServer server = draftService.requireServer(serverId);
        if (server.getStatus() != McpServerStatus.ARCHIVED) {
            throw new DomainBadRequestException("mcp.server.not_archived", serverId);
        }
        server.restore();
        server.setUpdatedBy(operator);
        return serverRepository.save(server);
    }
}
