-- 技能卡面中文展示：从 SKILL.md 正文提取的标题与描述。
-- headline 为 NULL 表示存量数据尚未回填；空串表示已处理但正文无可提取内容。
ALTER TABLE skill ADD COLUMN headline VARCHAR(200);
ALTER TABLE skill ADD COLUMN headline_summary TEXT;
