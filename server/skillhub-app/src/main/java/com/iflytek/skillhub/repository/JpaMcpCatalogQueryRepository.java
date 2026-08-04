package com.iflytek.skillhub.repository;

import com.iflytek.skillhub.domain.mcp.McpCategory;
import com.iflytek.skillhub.domain.mcp.McpCategoryRepository;
import com.iflytek.skillhub.domain.mcp.McpCategoryTranslation;
import com.iflytek.skillhub.domain.mcp.McpCategoryTranslationRepository;
import com.iflytek.skillhub.domain.mcp.McpServer;
import com.iflytek.skillhub.domain.mcp.McpServerRepository;
import com.iflytek.skillhub.domain.mcp.McpServerStatus;
import com.iflytek.skillhub.domain.mcp.McpVersion;
import com.iflytek.skillhub.domain.mcp.McpVersionCategory;
import com.iflytek.skillhub.domain.mcp.McpVersionCategoryRepository;
import com.iflytek.skillhub.domain.mcp.McpVersionRepository;
import com.iflytek.skillhub.domain.mcp.McpVisibility;
import com.iflytek.skillhub.domain.namespace.Namespace;
import com.iflytek.skillhub.domain.namespace.NamespaceRepository;
import com.iflytek.skillhub.dto.McpCategoryDto;
import com.iflytek.skillhub.dto.McpServerDetailResponse;
import com.iflytek.skillhub.dto.McpServerSummaryResponse;
import com.iflytek.skillhub.service.LocalizedTextResolver;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * MCP 广场读模型装配（多表投影）。直接使用 EntityManager 做参数化 JPQL，
 * 因为列表需要跨 mcp_server / mcp_version / namespace / category 多表 join，
 * 且公共可见性过滤条件（ACTIVE + 未隐藏 + PUBLIC + latest_version_id 非空）
 * 是展示关注点而非领域规则，不适合放入领域仓储。
 */
@Repository
public class JpaMcpCatalogQueryRepository implements McpCatalogQueryRepository {

    @PersistenceContext
    private EntityManager entityManager;

    private final McpVersionRepository versionRepository;
    private final McpVersionCategoryRepository versionCategoryRepository;
    private final McpCategoryRepository categoryRepository;
    private final McpCategoryTranslationRepository categoryTranslationRepository;
    private final NamespaceRepository namespaceRepository;
    private final LocalizedTextResolver localizedTextResolver;

    public JpaMcpCatalogQueryRepository(McpVersionRepository versionRepository,
                                        McpVersionCategoryRepository versionCategoryRepository,
                                        McpCategoryRepository categoryRepository,
                                        McpCategoryTranslationRepository categoryTranslationRepository,
                                        NamespaceRepository namespaceRepository,
                                        LocalizedTextResolver localizedTextResolver) {
        this.versionRepository = versionRepository;
        this.versionCategoryRepository = versionCategoryRepository;
        this.categoryRepository = categoryRepository;
        this.categoryTranslationRepository = categoryTranslationRepository;
        this.namespaceRepository = namespaceRepository;
        this.localizedTextResolver = localizedTextResolver;
    }

    @Override
    public Page<McpServerSummaryResponse> searchPublished(String keyword, String categorySlug,
                                                          Long namespaceId, String sort, Pageable pageable,
                                                          String locale) {
        StringBuilder jpql = new StringBuilder(
                "SELECT s FROM McpServer s, McpVersion v, Namespace n " +
                "WHERE v.id = s.latestVersionId AND n.id = s.namespaceId " +
                "AND s.status = 'ACTIVE' AND s.hidden = false AND s.visibility = 'PUBLIC' " +
                "AND s.latestVersionId IS NOT NULL");
        StringBuilder countJpql = new StringBuilder(
                "SELECT COUNT(s) FROM McpServer s " +
                "WHERE s.status = 'ACTIVE' AND s.hidden = false AND s.visibility = 'PUBLIC' " +
                "AND s.latestVersionId IS NOT NULL");

        Map<String, Object> params = new HashMap<>();
        if (keyword != null && !keyword.isBlank()) {
            jpql.append(" AND (LOWER(v.displayName) LIKE :kw OR LOWER(v.summary) LIKE :kw OR LOWER(s.slug) LIKE :kw)");
            countJpql.append(" AND EXISTS (SELECT 1 FROM McpVersion v WHERE v.id = s.latestVersionId " +
                    "AND (LOWER(v.displayName) LIKE :kw OR LOWER(v.summary) LIKE :kw OR LOWER(s.slug) LIKE :kw))");
            params.put("kw", "%" + keyword.trim().toLowerCase() + "%");
        }
        if (namespaceId != null) {
            jpql.append(" AND s.namespaceId = :nsId");
            countJpql.append(" AND s.namespaceId = :nsId");
            params.put("nsId", namespaceId);
        }
        if (categorySlug != null && !categorySlug.isBlank()) {
            jpql.append(" AND EXISTS (SELECT 1 FROM McpVersionCategory vc, McpCategory c " +
                    "WHERE vc.versionId = s.latestVersionId AND vc.categoryId = c.id AND c.slug = :catSlug)");
            countJpql.append(" AND EXISTS (SELECT 1 FROM McpVersionCategory vc, McpCategory c " +
                    "WHERE vc.versionId = s.latestVersionId AND vc.categoryId = c.id AND c.slug = :catSlug)");
            params.put("catSlug", categorySlug.trim());
        }

        jpql.append(sortClause(sort));

        TypedQuery<McpServer> query = entityManager.createQuery(jpql.toString(), McpServer.class);
        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql.toString(), Long.class);
        params.forEach((k, v) -> {
            query.setParameter(k, v);
            countQuery.setParameter(k, v);
        });
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<McpServer> servers = query.getResultList();
        long total = countQuery.getSingleResult();
        if (servers.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        return new PageImpl<>(toSummaries(servers, locale), pageable, total);
    }

    @Override
    public McpServerDetailResponse getPublishedDetail(Long namespaceId, String slug, String locale) {
        TypedQuery<McpServer> query = entityManager.createQuery(
                "SELECT s FROM McpServer s WHERE s.namespaceId = :nsId AND s.slug = :slug " +
                "AND s.status = 'ACTIVE' AND s.hidden = false AND s.visibility = 'PUBLIC' " +
                "AND s.latestVersionId IS NOT NULL", McpServer.class);
        query.setParameter("nsId", namespaceId);
        query.setParameter("slug", slug);
        List<McpServer> results = query.setMaxResults(1).getResultList();
        if (results.isEmpty()) {
            return null;
        }
        McpServer server = results.get(0);
        McpVersion version = versionRepository.findById(server.getLatestVersionId()).orElse(null);
        if (version == null) {
            return null;
        }
        Namespace namespace = namespaceRepository.findById(server.getNamespaceId()).orElse(null);
        String nsSlug = namespace != null ? namespace.getSlug() : "global";
        List<McpCategoryDto> categories = resolveCategories(List.of(version.getId()), locale)
                .getOrDefault(version.getId(), List.of());

        String coordinate = "@" + nsSlug + "/" + server.getSlug();
        String iconUrl = version.getIconObjectKey() != null
                ? "/api/v1/mcp/servers/" + nsSlug + "/" + server.getSlug() + "/icon" : null;
        String readmeUrl = "/api/v1/mcp/servers/" + nsSlug + "/" + server.getSlug() + "/readme";

        return new McpServerDetailResponse(
                server.getId(), nsSlug, server.getSlug(), coordinate,
                version.getDisplayName(), version.getSummary(),
                version.getPublisherName(), version.getPublisherUrl(),
                version.getHomepageUrl(), version.getRepositoryUrl(), version.getDocumentationUrl(),
                version.getLicenseSpdx(), version.getLicenseName(), version.getLicenseUrl(), version.getLicenseNote(),
                categories, iconUrl, readmeUrl,
                version.getUpstreamVersion(), version.getRevisionNo(),
                version.getSourceType() != null ? version.getSourceType().name() : null,
                version.getSourceUrl(), version.getSourceRevision(), version.getSourceFetchedAt(),
                version.getPublishedAt(), server.getUpdatedAt()
        );
    }

    private List<McpServerSummaryResponse> toSummaries(List<McpServer> servers, String locale) {
        List<Long> serverIds = servers.stream().map(McpServer::getId).toList();
        List<Long> versionIds = servers.stream().map(McpServer::getLatestVersionId).filter(Objects::nonNull).toList();
        List<Long> namespaceIds = servers.stream().map(McpServer::getNamespaceId).distinct().toList();

        Map<Long, McpVersion> versions = versionRepository.findByIdIn(versionIds).stream()
                .collect(Collectors.toMap(McpVersion::getId, Function.identity()));
        Map<Long, String> namespaceSlugs = namespaceRepository.findByIdIn(namespaceIds).stream()
                .collect(Collectors.toMap(Namespace::getId, Namespace::getSlug));
        Map<Long, List<McpCategoryDto>> categoriesByVersion = resolveCategories(versionIds, locale);

        List<McpServerSummaryResponse> result = new ArrayList<>(servers.size());
        for (McpServer server : servers) {
            McpVersion version = versions.get(server.getLatestVersionId());
            if (version == null) {
                continue;
            }
            String nsSlug = namespaceSlugs.getOrDefault(server.getNamespaceId(), "global");
            String iconUrl = version.getIconObjectKey() != null
                    ? "/api/v1/mcp/servers/" + nsSlug + "/" + server.getSlug() + "/icon" : null;
            result.add(new McpServerSummaryResponse(
                    server.getId(), nsSlug, server.getSlug(),
                    "@" + nsSlug + "/" + server.getSlug(),
                    version.getDisplayName(), version.getSummary(), version.getPublisherName(),
                    categoriesByVersion.getOrDefault(version.getId(), List.of()),
                    iconUrl, version.getUpstreamVersion(), server.getUpdatedAt()
            ));
        }
        return result;
    }

    private Map<Long, List<McpCategoryDto>> resolveCategories(List<Long> versionIds, String locale) {
        if (versionIds.isEmpty()) {
            return Map.of();
        }
        List<McpVersionCategory> links = versionCategoryRepository.findByVersionIdIn(versionIds);
        if (links.isEmpty()) {
            return Map.of();
        }
        List<Long> categoryIds = links.stream().map(McpVersionCategory::getCategoryId).distinct().toList();
        Map<Long, McpCategory> categories = categoryRepository.findByIdIn(categoryIds).stream()
                .collect(Collectors.toMap(McpCategory::getId, Function.identity()));
        Map<Long, List<McpCategoryTranslation>> translations = categoryTranslationRepository
                .findByCategoryIdIn(categoryIds).stream()
                .collect(Collectors.groupingBy(McpCategoryTranslation::getCategoryId));

        Map<Long, List<McpCategoryDto>> result = new HashMap<>();
        for (McpVersionCategory link : links) {
            McpCategory category = categories.get(link.getCategoryId());
            if (category == null || !category.isEnabled()) {
                continue;
            }
            List<McpCategoryTranslation> categoryTranslations = translations.getOrDefault(category.getId(), List.of());
            String displayName = localizedTextResolver.resolve(
                    categoryTranslations, McpCategoryTranslation::getLocale,
                    McpCategoryTranslation::getDisplayName, category.getSlug());
            String description = localizedTextResolver.resolve(
                    categoryTranslations, McpCategoryTranslation::getLocale,
                    t -> t.getDescription() != null ? t.getDescription() : "", "");
            result.computeIfAbsent(link.getVersionId(), k -> new ArrayList<>())
                    .add(new McpCategoryDto(category.getId(), category.getSlug(), displayName,
                            description.isBlank() ? null : description, link.getSortOrder()));
        }
        // 按 sortOrder 排序
        result.values().forEach(list -> list.sort(java.util.Comparator.comparing(
                c -> c.sortOrder() != null ? c.sortOrder() : 0)));
        return result;
    }

    private String sortClause(String sort) {
        // 稳定排序：始终以 id 作为第二排序键
        if ("name".equals(sort)) {
            return " ORDER BY s.slug ASC, s.id DESC";
        }
        // 默认 updated：以容器 updatedAt 倒序
        return " ORDER BY s.updatedAt DESC, s.id DESC";
    }
}
