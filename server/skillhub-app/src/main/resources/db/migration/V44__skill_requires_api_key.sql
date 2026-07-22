-- "需配置 API Key" 标识：版本级真源 + Skill 级冗余 + 搜索文档冗余
ALTER TABLE skill_version
    ADD COLUMN requires_api_key BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE skill
    ADD COLUMN requires_api_key BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE skill_search_document
    ADD COLUMN requires_api_key BOOLEAN NOT NULL DEFAULT FALSE;

-- 存量数据回填：frontmatter 显式声明 x-astron-requires-api-key: true 的版本
UPDATE skill_version
SET requires_api_key = TRUE
WHERE COALESCE(parsed_metadata_json->'frontmatter'->>'x-astron-requires-api-key', 'false') = 'true';

-- Skill 级冗余跟随 latest_version_id
UPDATE skill s
SET requires_api_key = TRUE
FROM skill_version sv
WHERE s.latest_version_id = sv.id
  AND sv.requires_api_key = TRUE;

-- 搜索文档冗余跟随 Skill
UPDATE skill_search_document d
SET requires_api_key = TRUE
FROM skill s
WHERE d.skill_id = s.id
  AND s.requires_api_key = TRUE;
