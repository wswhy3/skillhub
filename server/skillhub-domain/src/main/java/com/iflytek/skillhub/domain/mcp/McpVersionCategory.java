package com.iflytek.skillhub.domain.mcp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "mcp_version_category")
@IdClass(McpVersionCategoryId.class)
public class McpVersionCategory {

    @Id
    @Column(name = "version_id")
    private Long versionId;

    @Id
    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    protected McpVersionCategory() {}

    public McpVersionCategory(Long versionId, Long categoryId, Integer sortOrder) {
        this.versionId = versionId;
        this.categoryId = categoryId;
        this.sortOrder = sortOrder;
    }

    public Long getVersionId() { return versionId; }

    public Long getCategoryId() { return categoryId; }

    public Integer getSortOrder() { return sortOrder; }

    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
