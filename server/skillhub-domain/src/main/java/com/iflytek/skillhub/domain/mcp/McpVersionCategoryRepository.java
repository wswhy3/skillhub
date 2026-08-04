package com.iflytek.skillhub.domain.mcp;

import java.util.List;

public interface McpVersionCategoryRepository {

    McpVersionCategory save(McpVersionCategory versionCategory);

    List<McpVersionCategory> findByVersionId(Long versionId);

    List<McpVersionCategory> findByVersionIdIn(List<Long> versionIds);

    void deleteByVersionId(Long versionId);

    void flush();
}
