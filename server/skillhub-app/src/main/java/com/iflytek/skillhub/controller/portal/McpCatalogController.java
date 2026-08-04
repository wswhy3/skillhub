package com.iflytek.skillhub.controller.portal;

import com.iflytek.skillhub.controller.BaseApiController;
import com.iflytek.skillhub.domain.shared.exception.DomainNotFoundException;
import com.iflytek.skillhub.dto.ApiResponse;
import com.iflytek.skillhub.dto.ApiResponseFactory;
import com.iflytek.skillhub.dto.McpCategoryDto;
import com.iflytek.skillhub.dto.McpServerDetailResponse;
import com.iflytek.skillhub.dto.McpServerSummaryResponse;
import com.iflytek.skillhub.dto.PageResponse;
import com.iflytek.skillhub.ratelimit.RateLimit;
import com.iflytek.skillhub.service.McpAssetAppService;
import com.iflytek.skillhub.service.McpCatalogAppService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * MCP 广场公共接口：列表、分类、详情、README、图标。
 * 全部匿名可访问，公共 GET 支持 API Token 读。
 */
@RestController
@RequestMapping("/api/v1/mcp")
public class McpCatalogController extends BaseApiController {

    private static final Pattern NON_NEGATIVE_INTEGER = Pattern.compile("\\d+");
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 24;
    private static final String DEFAULT_SORT = "updated";

    private final McpCatalogAppService catalogAppService;
    private final McpAssetAppService assetAppService;

    public McpCatalogController(McpCatalogAppService catalogAppService,
                                McpAssetAppService assetAppService,
                                ApiResponseFactory responseFactory) {
        super(responseFactory);
        this.catalogAppService = catalogAppService;
        this.assetAppService = assetAppService;
    }

    @GetMapping("/categories")
    @RateLimit(category = "mcp-public", authenticated = 60, anonymous = 30)
    public ApiResponse<List<McpCategoryDto>> listCategories() {
        return ok("response.success.read", catalogAppService.listCategories());
    }

    @GetMapping("/servers")
    @RateLimit(category = "mcp-public", authenticated = 60, anonymous = 30)
    public ApiResponse<PageResponse<McpServerSummaryResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String namespace,
            @Parameter(schema = @Schema(defaultValue = DEFAULT_SORT))
            @RequestParam(required = false) String sort,
            @Parameter(schema = @Schema(type = "integer", defaultValue = "0", minimum = "0"))
            @RequestParam(required = false) String page,
            @Parameter(schema = @Schema(type = "integer", defaultValue = "24", minimum = "1", maximum = "48"))
            @RequestParam(required = false) String size) {
        PageResponse<McpServerSummaryResponse> result = catalogAppService.search(
                keyword, category, namespace, normalizeSort(sort),
                parseNonNegativeInt(page, DEFAULT_PAGE), parsePositiveInt(size, DEFAULT_SIZE));
        return ok("response.success.read", result);
    }

    @GetMapping("/servers/{namespace}/{slug}")
    @RateLimit(category = "mcp-public", authenticated = 60, anonymous = 30)
    public ApiResponse<McpServerDetailResponse> detail(@PathVariable String namespace,
                                                       @PathVariable String slug) {
        McpServerDetailResponse detail = catalogAppService.getDetail(namespace, slug);
        if (detail == null) {
            throw new DomainNotFoundException("mcp.server.not_found", slug);
        }
        return ok("response.success.read", detail);
    }

    @GetMapping("/servers/{namespace}/{slug}/readme")
    @RateLimit(category = "mcp-public", authenticated = 60, anonymous = 30)
    public ResponseEntity<InputStreamResource> readme(
            @PathVariable String namespace,
            @PathVariable String slug,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        McpAssetAppService.AssetContent asset = assetAppService.loadReadme(namespace, slug);
        if (asset == null) {
            throw new DomainNotFoundException("mcp.server.not_found", slug);
        }
        return buildAssetResponse(asset, ifNoneMatch, MediaType.parseMediaType("text/markdown"));
    }

    @GetMapping("/servers/{namespace}/{slug}/icon")
    @RateLimit(category = "mcp-public", authenticated = 60, anonymous = 30)
    public ResponseEntity<InputStreamResource> icon(
            @PathVariable String namespace,
            @PathVariable String slug,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        McpAssetAppService.AssetContent asset = assetAppService.loadIcon(namespace, slug);
        if (asset == null) {
            throw new DomainNotFoundException("mcp.server.not_found", slug);
        }
        return buildAssetResponse(asset, ifNoneMatch, MediaType.parseMediaType(asset.contentType()));
    }

    private ResponseEntity<InputStreamResource> buildAssetResponse(McpAssetAppService.AssetContent asset,
                                                                   String ifNoneMatch, MediaType mediaType) {
        String etag = asset.sha256() != null ? "\"" + asset.sha256() + "\"" : null;
        if (etag != null && matchesEtag(etag, ifNoneMatch)) {
            return ResponseEntity.status(304).eTag(etag).build();
        }
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic());
        if (etag != null) {
            builder.eTag(etag);
        }
        if (asset.size() != null) {
            builder.contentLength(asset.size());
        }
        return builder.body(new InputStreamResource(asset.stream()));
    }

    private boolean matchesEtag(String currentEtag, String ifNoneMatch) {
        if (ifNoneMatch == null || ifNoneMatch.isBlank()) {
            return false;
        }
        String normalizedCurrent = normalizeEtagToken(currentEtag);
        for (String candidate : ifNoneMatch.split(",")) {
            String normalizedCandidate = normalizeEtagToken(candidate);
            if ("*".equals(normalizedCandidate) || normalizedCurrent.equals(normalizedCandidate)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeEtagToken(String etag) {
        if (etag == null) {
            return "";
        }
        String normalized = etag.trim();
        if (normalized.regionMatches(true, 0, "W/", 0, 2)) {
            normalized = normalized.substring(2).trim();
        }
        if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized;
    }

    private String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return DEFAULT_SORT;
        }
        return sort.trim();
    }

    private int parseNonNegativeInt(String rawValue, int defaultValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return defaultValue;
        }
        String normalized = rawValue.trim();
        if (!NON_NEGATIVE_INTEGER.matcher(normalized).matches()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private int parsePositiveInt(String rawValue, int defaultValue) {
        int parsed = parseNonNegativeInt(rawValue, defaultValue);
        return parsed > 0 ? parsed : defaultValue;
    }
}
