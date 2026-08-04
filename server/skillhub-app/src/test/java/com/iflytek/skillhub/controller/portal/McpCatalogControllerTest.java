package com.iflytek.skillhub.controller.portal;

import com.iflytek.skillhub.domain.namespace.NamespaceMemberRepository;
import com.iflytek.skillhub.dto.McpCategoryDto;
import com.iflytek.skillhub.dto.McpServerDetailResponse;
import com.iflytek.skillhub.dto.McpServerSummaryResponse;
import com.iflytek.skillhub.dto.PageResponse;
import com.iflytek.skillhub.service.McpAssetAppService;
import com.iflytek.skillhub.service.McpCatalogAppService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class McpCatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private McpCatalogAppService catalogAppService;

    @MockBean
    private McpAssetAppService assetAppService;

    @MockBean
    private NamespaceMemberRepository namespaceMemberRepository;

    @Test
    void listCategoriesIsAnonymous() throws Exception {
        when(catalogAppService.listCategories())
                .thenReturn(List.of(new McpCategoryDto(1L, "devtools", "Dev Tools", null, 0)));

        mockMvc.perform(get("/api/v1/mcp/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].slug").value("devtools"))
                .andExpect(jsonPath("$.data[0].displayName").value("Dev Tools"));
    }

    @Test
    void searchServersIsAnonymous() throws Exception {
        McpServerSummaryResponse summary = new McpServerSummaryResponse(
                1L, "global", "my-mcp", "@global/my-mcp", "My MCP", "A test MCP",
                "Acme", List.of(), null, "1.0.0", Instant.now());
        when(catalogAppService.search(any(), any(), any(), anyString(), anyInt(), anyInt()))
                .thenReturn(new PageResponse<>(List.of(summary), 1, 0, 24));

        mockMvc.perform(get("/api/v1/mcp/servers").param("keyword", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items[0].coordinate").value("@global/my-mcp"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void detailReturns404WhenNotVisible() throws Exception {
        when(catalogAppService.getDetail(anyString(), anyString())).thenReturn(null);

        mockMvc.perform(get("/api/v1/mcp/servers/global/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void readmeReturnsMarkdownWithETag() throws Exception {
        byte[] body = "# Hello".getBytes();
        McpAssetAppService.AssetContent asset = new McpAssetAppService.AssetContent(
                new ByteArrayInputStream(body), "text/markdown; charset=UTF-8", "abc123", (long) body.length);
        when(assetAppService.loadReadme("global", "my-mcp")).thenReturn(asset);

        mockMvc.perform(get("/api/v1/mcp/servers/global/my-mcp/readme"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"abc123\""))
                .andExpect(content().contentTypeCompatibleWith("text/markdown"));
    }

    @Test
    void readmeReturns304WhenETagMatches() throws Exception {
        McpAssetAppService.AssetContent asset = new McpAssetAppService.AssetContent(
                new ByteArrayInputStream("x".getBytes()), "text/markdown; charset=UTF-8", "abc123", 1L);
        when(assetAppService.loadReadme("global", "my-mcp")).thenReturn(asset);

        mockMvc.perform(get("/api/v1/mcp/servers/global/my-mcp/readme")
                        .header("If-None-Match", "\"abc123\""))
                .andExpect(status().isNotModified());
    }

    @Test
    void readmeReturns304WhenIfNoneMatchUsesWeakOrMultipleEtags() throws Exception {
        McpAssetAppService.AssetContent asset = new McpAssetAppService.AssetContent(
                new ByteArrayInputStream("x".getBytes()), "text/markdown; charset=UTF-8", "abc123", 1L);
        when(assetAppService.loadReadme("global", "my-mcp")).thenReturn(asset);

        mockMvc.perform(get("/api/v1/mcp/servers/global/my-mcp/readme")
                        .header("If-None-Match", "W/\"zzz\", W/\"abc123\""))
                .andExpect(status().isNotModified());
    }

    @Test
    void iconReturns404WhenMissing() throws Exception {
        when(assetAppService.loadIcon("global", "my-mcp")).thenReturn(null);

        mockMvc.perform(get("/api/v1/mcp/servers/global/my-mcp/icon"))
                .andExpect(status().isNotFound());
    }
}
