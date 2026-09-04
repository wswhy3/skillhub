# Higress AI 网关 — MCP Server 部署指南

## 概述

本配置将 SkillHub 的核心 REST API 通过 Higress AI 网关暴露为 MCP Server，使 AI Agent（Cursor、Claude、WorkBuddy、Codex、Trae 等）可以直接通过 MCP 协议搜索、查看和下载技能。

## 文件说明

| 文件 | 用途 |
|------|------|
| `mcp-server-skillhub.yaml` | MCP Server 工具规范配置（定义 4 个工具） |
| `docker-compose.higress.yml` | 本地开发/测试用的 Higress 容器定义 |

## 暴露的 MCP 工具

| 工具名 | 对应 SkillHub API | 功能 |
|--------|-------------------|------|
| `search-skills` | `GET /api/web/skills` | 按关键词搜索技能 |
| `get-skill-detail` | `GET /api/v1/skills/{namespace}/{slug}` | 查看技能详情 |
| `download-skill` | `GET /api/v1/skills/{namespace}/{slug}/download` | 下载技能包 |
| `list-namespaces` | `GET /api/v1/namespaces` | 列出命名空间 |

## 一、本地开发环境部署

### 1. 启动 Higress

```bash
# 启动 Higress（与 SkillHub 依赖服务并行）
docker compose -f docker-compose.yml -f docker-compose.higress.yml up -d higress

# 确认启动成功
docker compose -f docker-compose.yml -f docker-compose.higress.yml ps higress
```

### 2. 配置 MCP Server

1. 打开浏览器访问 `http://localhost:8001`，首次登录设置管理员密码
2. **添加服务来源**：在"服务来源"中添加一个 DNS 或静态服务，指向 `skillhub-server:8080`
3. **创建 MCP Server**：
   - 进入 MCP 管理 → 创建 MCP 服务
   - 类型选择 OpenAPI
   - 将 `mcp-server-skillhub.yaml` 的内容粘贴到工具规范编辑器
   - 保存

### 3. 验证

```bash
# 测试 MCP Server 端点是否可达
curl http://localhost:8080/mcp-servers/skillhub/sse

# 测试搜索工具（通过网关直接调用 SkillHub API）
curl 'http://localhost:8080/api/web/skills?q=code-review'
```

## 二、生产环境部署

### 1. 部署 Higress

参考 [Higress 官方部署文档](https://higress.cn/docs/latest/user/quickstart/)，在 K8s 集群中部署 Higress。

```bash
# K8s 部署（需要 Higress 2.1.0+）
helm install higress -n higress-system \
  higress.io/higress \
  --set global.enableRedis=true \
  --set higress.core.gateway.http.port=8080
```

### 2. 配置服务来源

在 Higress 控制台中添加服务来源，指向 SkillHub 后端服务地址：
- K8s 集群内：`skillhub-server.skillhub.svc.cluster.local:8080`
- 外部访问：通过 Ingress 暴露的 SkillHub API 地址

### 3. 导入 MCP Server 配置

在 Higress 控制台 → MCP 管理 → 创建 OpenAPI 类型的 MCP Server，粘贴 `mcp-server-skillhub.yaml` 内容。

### 4. 配置路由和认证（可选但推荐）

- 在 AI 路由管理中为 MCP Server 配置域名
- 配置认证策略（API Key / Bearer Token）
- 配置限流策略（防止 AI Agent 过度调用）

## 三、AI Agent 端配置

MCP Server 配置完成后，各 AI 工具使用以下地址连接：

```
http://<higress-gateway-address>:8080/mcp-servers/skillhub/sse
```

### Cursor

Settings → Tools & Integrations → Add Custom MCP：

```json
{
  "mcpServers": {
    "skillhub": {
      "url": "http://<higress-address>:8080/mcp-servers/skillhub/sse"
    }
  }
}
```

### Claude Desktop / CLI

在 MCP 配置文件中添加：

```json
{
  "mcpServers": {
    "skillhub": {
      "url": "http://<higress-address>:8080/mcp-servers/skillhub/sse"
    }
  }
}
```

### WorkBuddy

在 WorkBuddy 的 `mcpServers` 配置中添加：

```json
{
  "mcpServers": {
    "skillhub": {
      "type": "sse",
      "url": "http://<higress-address>:8080/mcp-servers/skillhub/sse"
    }
  }
}
```

### Codex / Trae

同 Claude Desktop 配置方式，在各自的 MCP 配置中添加相同的 `url`。

## 四、配置参数说明

`mcp-server-skillhub.yaml` 中的关键配置项：

| 配置项 | 说明 | 需要修改的场景 |
|--------|------|---------------|
| `server.name` | MCP Server 名称，保持 `skillhub` | 不需要改 |
| `url` 中的 `skillhub-server:8080` | SkillHub 后端地址 | 部署环境不同时改为实际地址 |
| `X-Mock-User-Id` header | 本地开发模拟用户头 | 生产环境改为真实认证方式 |
| `responseTemplate` | 响应模板（Go template） | API 返回结构变化时同步更新 |

## 五、安全注意事项

1. **生产环境必须移除 `X-Mock-User-Id` header**，改为通过 Higress 路由策略配置真实认证
2. 只暴露只读 API（搜索、详情、下载），不暴露发布、删除、审核等管理操作
3. 建议在 Higress AI 路由中配置限流策略（如每分钟 30 次调用）
4. MCP Server 端点建议通过 HTTPS 暴露
