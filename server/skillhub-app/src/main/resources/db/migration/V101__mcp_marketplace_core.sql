-- MCP 广场核心表（独立领域，不修改 Skill 现有表）
-- 包含：mcp_server / mcp_version / mcp_category / mcp_category_translation
--       / mcp_version_category / mcp_submission
-- 说明：数据库从第一天起保存 namespace_id，P0 仅开放 @global + PUBLIC。

CREATE TABLE mcp_server (
    id BIGSERIAL PRIMARY KEY,
    namespace_id BIGINT NOT NULL REFERENCES namespace (id) ON DELETE RESTRICT,
    slug VARCHAR(100) NOT NULL,
    owner_id VARCHAR(128) NOT NULL,
    visibility VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    latest_version_id BIGINT,
    hidden BOOLEAN NOT NULL DEFAULT FALSE,
    hidden_at TIMESTAMPTZ,
    hidden_by VARCHAR(128),
    lock_version INT NOT NULL DEFAULT 0,
    created_by VARCHAR(128) NOT NULL,
    updated_by VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_mcp_server_namespace_slug UNIQUE (namespace_id, slug),
    CONSTRAINT ck_mcp_server_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT ck_mcp_server_visibility CHECK (visibility IN ('PUBLIC', 'NAMESPACE_ONLY', 'PRIVATE'))
);

-- 公开列表部分索引：容器 ACTIVE、未隐藏、PUBLIC、已有发布版本
CREATE INDEX idx_mcp_server_public ON mcp_server (updated_at DESC, id DESC)
    WHERE status = 'ACTIVE' AND hidden = FALSE AND visibility = 'PUBLIC' AND latest_version_id IS NOT NULL;

CREATE TABLE mcp_version (
    id BIGSERIAL PRIMARY KEY,
    server_id BIGINT NOT NULL REFERENCES mcp_server (id) ON DELETE RESTRICT,
    revision_no INT NOT NULL,
    upstream_version VARCHAR(128),
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    content_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    display_name VARCHAR(200) NOT NULL,
    summary TEXT NOT NULL,
    publisher_name VARCHAR(200),
    publisher_url VARCHAR(1024),
    homepage_url VARCHAR(1024),
    repository_url VARCHAR(1024),
    documentation_url VARCHAR(1024),
    license_spdx VARCHAR(64),
    license_name VARCHAR(128),
    license_url VARCHAR(1024),
    license_note TEXT,
    source_type VARCHAR(32) NOT NULL,
    source_url VARCHAR(2048),
    source_revision VARCHAR(256),
    source_fetched_at TIMESTAMPTZ,
    source_sha256 VARCHAR(64),
    readme_object_key VARCHAR(512),
    readme_sha256 VARCHAR(64),
    readme_size BIGINT,
    icon_object_key VARCHAR(512),
    icon_sha256 VARCHAR(64),
    icon_content_type VARCHAR(128),
    icon_size BIGINT,
    content_error_code VARCHAR(64),
    content_error_message VARCHAR(512),
    lock_version INT NOT NULL DEFAULT 0,
    published_at TIMESTAMPTZ,
    yanked_at TIMESTAMPTZ,
    yanked_by VARCHAR(128),
    yank_reason TEXT,
    created_by VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_mcp_version_server_revision UNIQUE (server_id, revision_no),
    CONSTRAINT ck_mcp_version_revision_positive CHECK (revision_no > 0),
    CONSTRAINT ck_mcp_version_readme_size CHECK (readme_size IS NULL OR readme_size >= 0),
    CONSTRAINT ck_mcp_version_icon_size CHECK (icon_size IS NULL OR icon_size >= 0),
    CONSTRAINT ck_mcp_version_status CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'PUBLISHED', 'REJECTED', 'YANKED')),
    CONSTRAINT ck_mcp_version_content_status CHECK (content_status IN ('PENDING', 'READY', 'FAILED')),
    CONSTRAINT ck_mcp_version_source_type CHECK (source_type IN ('MANUAL', 'GITHUB_REPOSITORY', 'REMOTE_HTTP'))
);

CREATE INDEX idx_mcp_version_server_status ON mcp_version (server_id, status, revision_no DESC);
CREATE INDEX idx_mcp_version_status_created ON mcp_version (status, created_at DESC);

-- latest_version_id 外键在 mcp_version 建表后补充
ALTER TABLE mcp_server
    ADD CONSTRAINT fk_mcp_server_latest_version
    FOREIGN KEY (latest_version_id) REFERENCES mcp_version (id) ON DELETE SET NULL;

CREATE TABLE mcp_category (
    id BIGSERIAL PRIMARY KEY,
    slug VARCHAR(100) NOT NULL UNIQUE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_by VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE mcp_category_translation (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES mcp_category (id) ON DELETE CASCADE,
    locale VARCHAR(16) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    description TEXT,
    CONSTRAINT uq_mcp_category_translation UNIQUE (category_id, locale)
);

CREATE INDEX idx_mcp_category_translation_category ON mcp_category_translation (category_id);

-- 分类绑定在版本快照上，保证审核人看到的分类与最终发布内容一致
CREATE TABLE mcp_version_category (
    version_id BIGINT NOT NULL REFERENCES mcp_version (id) ON DELETE CASCADE,
    category_id BIGINT NOT NULL REFERENCES mcp_category (id) ON DELETE RESTRICT,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (version_id, category_id)
);

CREATE INDEX idx_mcp_version_category_category ON mcp_version_category (category_id);

CREATE TABLE mcp_submission (
    id BIGSERIAL PRIMARY KEY,
    version_id BIGINT NOT NULL REFERENCES mcp_version (id) ON DELETE RESTRICT,
    namespace_id BIGINT NOT NULL REFERENCES namespace (id) ON DELETE RESTRICT,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    lock_version INT NOT NULL DEFAULT 0,
    submitted_by VARCHAR(128) NOT NULL,
    reviewed_by VARCHAR(128),
    review_comment TEXT,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMPTZ,
    CONSTRAINT ck_mcp_submission_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

-- 同一版本同一时刻只允许一个待审核提交
CREATE UNIQUE INDEX uq_mcp_submission_pending ON mcp_submission (version_id) WHERE status = 'PENDING';

CREATE INDEX idx_mcp_submission_status ON mcp_submission (status, submitted_at DESC);
