package com.iflytek.skillhub.service;

import com.iflytek.skillhub.domain.mcp.McpServer;
import com.iflytek.skillhub.domain.mcp.McpServerRepository;
import com.iflytek.skillhub.domain.mcp.McpSubmission;
import com.iflytek.skillhub.domain.mcp.McpSubmissionRepository;
import com.iflytek.skillhub.domain.mcp.McpVersion;
import com.iflytek.skillhub.domain.mcp.McpVersionRepository;
import com.iflytek.skillhub.domain.mcp.service.McpDraftService;
import com.iflytek.skillhub.domain.mcp.service.McpGovernanceService;
import com.iflytek.skillhub.domain.namespace.Namespace;
import com.iflytek.skillhub.domain.namespace.NamespaceRepository;
import com.iflytek.skillhub.dto.McpAdminServerResponse;
import com.iflytek.skillhub.dto.PageResponse;
import com.iflytek.skillhub.storage.ObjectStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * MCP 治理编排：下架、隐藏、归档 + 管理列表。
 */
@Service
public class McpAdminGovernanceAppService {

    private static final Logger log = LoggerFactory.getLogger(McpAdminGovernanceAppService.class);

    private final McpGovernanceService governanceService;
    private final McpDraftService draftService;
    private final McpServerRepository serverRepository;
    private final McpVersionRepository versionRepository;
    private final McpSubmissionRepository submissionRepository;
    private final NamespaceRepository namespaceRepository;
    private final ObjectStorageService objectStorageService;
    private final McpAdminAuditService auditService;

    public McpAdminGovernanceAppService(McpGovernanceService governanceService,
                                        McpDraftService draftService,
                                        McpServerRepository serverRepository,
                                        McpVersionRepository versionRepository,
                                        McpSubmissionRepository submissionRepository,
                                        NamespaceRepository namespaceRepository,
                                        ObjectStorageService objectStorageService,
                                        McpAdminAuditService auditService) {
        this.governanceService = governanceService;
        this.draftService = draftService;
        this.serverRepository = serverRepository;
        this.versionRepository = versionRepository;
        this.submissionRepository = submissionRepository;
        this.namespaceRepository = namespaceRepository;
        this.objectStorageService = objectStorageService;
        this.auditService = auditService;
    }

    @Transactional
    public void yank(Long versionId, String reason, String operator, String clientIp, String userAgent) {
        governanceService.yank(versionId, operator, reason);
        auditService.record(operator, "MCP_YANK", versionId,
                auditService.detail("reason", reason), clientIp, userAgent);
    }

    @Transactional
    public void hide(Long serverId, String reason, String operator, String clientIp, String userAgent) {
        governanceService.hide(serverId, operator);
        auditService.record(operator, "MCP_HIDE", serverId,
                auditService.detail("reason", reason), clientIp, userAgent);
    }

    @Transactional
    public void unhide(Long serverId, String operator, String clientIp, String userAgent) {
        governanceService.unhide(serverId, operator);
        auditService.record(operator, "MCP_UNHIDE", serverId, Map.of(), clientIp, userAgent);
    }

    @Transactional
    public void archive(Long serverId, String operator, String clientIp, String userAgent) {
        governanceService.archive(serverId, operator);
        auditService.record(operator, "MCP_ARCHIVE", serverId, Map.of(), clientIp, userAgent);
    }

    @Transactional
    public void restore(Long serverId, String operator, String clientIp, String userAgent) {
        governanceService.restore(serverId, operator);
        auditService.record(operator, "MCP_RESTORE", serverId, Map.of(), clientIp, userAgent);
    }

    @Transactional
    public void deleteServer(Long serverId, String operator, String clientIp, String userAgent) {
        McpServer server = draftService.requireServer(serverId);
        List<McpVersion> versions = versionRepository.findByServerId(serverId);
        List<String> storageKeys = collectStorageKeys(versions);

        // Break the published-version pointer first so deleting versions does not
        // leave the container row referencing an about-to-be-removed version.
        if (server.getLatestVersionId() != null) {
            server.setLatestVersionId(null);
            server.setUpdatedBy(operator);
            serverRepository.save(server);
            serverRepository.flush();
        }

        for (McpVersion version : versions) {
            List<McpSubmission> submissions = submissionRepository.findByVersionIdOrderBySubmittedAtDesc(version.getId());
            for (McpSubmission submission : submissions) {
                submissionRepository.delete(submission);
            }
            versionRepository.delete(version);
            versionRepository.flush();
        }
        serverRepository.delete(server);
        serverRepository.flush();

        deleteStorageAfterCommit(server, storageKeys);
        auditService.record(operator, "MCP_DELETE", serverId,
                auditService.detail("slug", server.getSlug(), "versionCount", versions.size()),
                clientIp, userAgent);
    }

    @Transactional(readOnly = true)
    public PageResponse<McpAdminServerResponse> listServers(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<McpServer> result = serverRepository.findAll(pageable);
        return PageResponse.from(result.map(this::toResponse));
    }

    private McpAdminServerResponse toResponse(McpServer server) {
        String namespaceSlug = namespaceRepository.findById(server.getNamespaceId())
                .map(Namespace::getSlug).orElse("global");
        McpVersion latestDraft = versionRepository.findByServerId(server.getId()).stream()
                .filter(McpVersion::isEditable)
                .max(java.util.Comparator.comparing(McpVersion::getRevisionNo))
                .orElse(null);
        return new McpAdminServerResponse(
                server.getId(), namespaceSlug, server.getSlug(),
                server.getStatus().name(), server.getVisibility().name(), server.isHidden(),
                server.getLatestVersionId(),
                latestDraft != null ? latestDraft.getId() : null,
                latestDraft != null ? latestDraft.getDisplayName() : null,
                server.getUpdatedAt());
    }

    private List<String> collectStorageKeys(List<McpVersion> versions) {
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (McpVersion version : versions) {
            if (version.getReadmeObjectKey() != null && !version.getReadmeObjectKey().isBlank()) {
                keys.add(version.getReadmeObjectKey());
            }
            if (version.getIconObjectKey() != null && !version.getIconObjectKey().isBlank()) {
                keys.add(version.getIconObjectKey());
            }
        }
        return new ArrayList<>(keys);
    }

    private void deleteStorageAfterCommit(McpServer server, List<String> storageKeys) {
        if (storageKeys.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteStorageObjects(server, storageKeys);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteStorageObjects(server, storageKeys);
            }
        });
    }

    private void deleteStorageObjects(McpServer server, List<String> storageKeys) {
        try {
            objectStorageService.deleteObjects(storageKeys);
        } catch (RuntimeException ex) {
            log.error("Failed to delete MCP storage objects after deleting server [serverId={}, slug={}, keys={}]",
                    server.getId(), server.getSlug(), storageKeys, ex);
        }
    }
}
