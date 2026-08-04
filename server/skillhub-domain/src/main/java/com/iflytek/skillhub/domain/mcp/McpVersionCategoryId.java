package com.iflytek.skillhub.domain.mcp;

import java.io.Serializable;
import java.util.Objects;

public class McpVersionCategoryId implements Serializable {

    private Long versionId;
    private Long categoryId;

    public McpVersionCategoryId() {}

    public McpVersionCategoryId(Long versionId, Long categoryId) {
        this.versionId = versionId;
        this.categoryId = categoryId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof McpVersionCategoryId that)) return false;
        return Objects.equals(versionId, that.versionId) && Objects.equals(categoryId, that.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(versionId, categoryId);
    }
}
