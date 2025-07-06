package com.example.customerservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 知识文档实体类
 * 
 * 存储知识库中的文档内容，包括标题、内容、分类、标签等信息。
 * 支持文档版本管理、状态控制和访问统计功能。
 * 
 * @author AI Assistant
 * @version 1.0.0
 * @since 2025-07-06
 */
@Entity
@Table(name = "knowledge_documents", indexes = {
    @Index(name = "idx_category_id", columnList = "category_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_title", columnList = "title")
})
public class KnowledgeDocument {

    /**
     * 文档唯一标识符
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 文档标题
     * 必填字段，用于显示和搜索
     */
    @NotBlank(message = "文档标题不能为空")
    @Size(min = 1, max = 200, message = "文档标题长度必须在1-200字符之间")
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * 文档内容
     * 支持富文本格式，存储完整的文档内容
     */
    @NotBlank(message = "文档内容不能为空")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 文档摘要
     * 用于快速预览和搜索结果显示
     */
    @Size(max = 1000, message = "文档摘要长度不能超过1000字符")
    @Column(length = 1000)
    private String summary;

    /**
     * 所属分类
     * 多对一关系，一个文档属于一个分类
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private KnowledgeCategory category;

    /**
     * 文档标签
     * 以逗号分隔的标签字符串，用于分类和搜索
     */
    @Column(length = 500)
    private String tags;

    /**
     * 文档状态
     * DRAFT: 草稿状态，未发布
     * PUBLISHED: 已发布，可被搜索和使用
     * ARCHIVED: 已归档，不再显示但保留数据
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status = DocumentStatus.DRAFT;

    /**
     * 文档版本号
     * 从1开始，每次更新递增
     */
    @Column(nullable = false)
    private Integer version = 1;

    /**
     * 文档优先级
     * 用于搜索结果排序，数值越高优先级越高
     */
    @Column(nullable = false)
    private Integer priority = 0;

    /**
     * 访问次数
     * 统计文档被查看的次数
     */
    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    /**
     * 点赞次数
     * 用户对文档的点赞统计
     */
    @Column(name = "like_count", nullable = false)
    private Long likeCount = 0L;

    /**
     * 创建者
     */
    @Column(name = "created_by", length = 100)
    private String createdBy;

    /**
     * 最后更新者
     */
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 最后更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 发布时间
     * 文档首次发布的时间
     */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /**
     * 搜索索引
     * 一对一关系，每个文档对应一个搜索索引
     */
    @OneToOne(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private KnowledgeSearchIndex searchIndex;

    /**
     * 文档状态枚举
     */
    public enum DocumentStatus {
        /**
         * 草稿状态 - 文档正在编辑中，未发布
         */
        DRAFT,
        
        /**
         * 已发布状态 - 文档已发布，可被搜索和使用
         */
        PUBLISHED,
        
        /**
         * 已归档状态 - 文档已归档，不再显示但保留数据
         */
        ARCHIVED
    }

    /**
     * 默认构造函数
     */
    public KnowledgeDocument() {
    }

    /**
     * 构造函数
     * 
     * @param title 文档标题
     * @param content 文档内容
     * @param category 所属分类
     */
    public KnowledgeDocument(String title, String content, KnowledgeCategory category) {
        this.title = title;
        this.content = content;
        this.category = category;
    }

    /**
     * JPA生命周期回调 - 持久化前执行
     */
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * JPA生命周期回调 - 更新前执行
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 发布文档
     * 将文档状态设置为已发布，并记录发布时间
     */
    public void publish() {
        this.status = DocumentStatus.PUBLISHED;
        if (this.publishedAt == null) {
            this.publishedAt = LocalDateTime.now();
        }
    }

    /**
     * 归档文档
     * 将文档状态设置为已归档
     */
    public void archive() {
        this.status = DocumentStatus.ARCHIVED;
    }

    /**
     * 增加访问次数
     */
    public void incrementViewCount() {
        this.viewCount++;
    }

    /**
     * 增加点赞次数
     */
    public void incrementLikeCount() {
        this.likeCount++;
    }

    /**
     * 获取标签列表
     * 
     * @return 标签列表
     */
    public List<String> getTagList() {
        if (tags == null || tags.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(tags.split(","));
    }

    /**
     * 设置标签列表
     * 
     * @param tagList 标签列表
     */
    public void setTagList(List<String> tagList) {
        if (tagList == null || tagList.isEmpty()) {
            this.tags = null;
        } else {
            this.tags = String.join(",", tagList);
        }
    }

    /**
     * 判断文档是否已发布
     * 
     * @return true如果已发布，false否则
     */
    public boolean isPublished() {
        return DocumentStatus.PUBLISHED.equals(this.status);
    }

    /**
     * 判断文档是否为草稿
     * 
     * @return true如果是草稿，false否则
     */
    public boolean isDraft() {
        return DocumentStatus.DRAFT.equals(this.status);
    }

    /**
     * 判断文档是否已归档
     * 
     * @return true如果已归档，false否则
     */
    public boolean isArchived() {
        return DocumentStatus.ARCHIVED.equals(this.status);
    }

    // Getter和Setter方法

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public KnowledgeCategory getCategory() {
        return category;
    }

    public void setCategory(KnowledgeCategory category) {
        this.category = category;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Long getViewCount() {
        return viewCount;
    }

    public void setViewCount(Long viewCount) {
        this.viewCount = viewCount;
    }

    public Long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Long likeCount) {
        this.likeCount = likeCount;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public KnowledgeSearchIndex getSearchIndex() {
        return searchIndex;
    }

    public void setSearchIndex(KnowledgeSearchIndex searchIndex) {
        this.searchIndex = searchIndex;
    }

    @Override
    public String toString() {
        return "KnowledgeDocument{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", version=" + version +
                ", viewCount=" + viewCount +
                ", createdAt=" + createdAt +
                '}';
    }
}
