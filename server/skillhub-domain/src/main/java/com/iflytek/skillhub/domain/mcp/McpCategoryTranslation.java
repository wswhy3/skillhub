package com.iflytek.skillhub.domain.mcp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "mcp_category_translation")
public class McpCategoryTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(nullable = false, length = 16)
    private String locale;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String description;

    protected McpCategoryTranslation() {}

    public McpCategoryTranslation(Long categoryId, String locale, String displayName) {
        this.categoryId = categoryId;
        this.locale = locale;
        this.displayName = displayName;
    }

    public Long getId() { return id; }

    public Long getCategoryId() { return categoryId; }

    public String getLocale() { return locale; }

    public void setLocale(String locale) { this.locale = locale; }

    public String getDisplayName() { return displayName; }

    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }
}
