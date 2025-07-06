package com.example.customerservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识分类实体类
 * 
 * 用于组织和管理知识库中的文档分类，支持层级分类结构。
 * 每个分类可以包含多个子分类和多个知识文档。
 * 
 * @author AI Assistant
 * @version 1.0.0
 * @since 2025-07-06
 */
@Entity
@Table(name = "knowledge_categories")
public class KnowledgeCategory {

    /**
     * 分类唯一标识符
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 分类名称
     * 必填字段，长度限制在1-100字符之间
     */
    @NotBlank(message = "分类名称不能为空")
    @Size(min = 1, max = 100, message = "分类名称长度必须在1-100字符之间")
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 分类描述
     * 可选字段，用于详细说明分类的用途和范围
     */
    @Size(max = 500, message = "分类描述长度不能超过500字符")
    @Column(length = 500)
    private String description;

    /**
     * 父分类ID
     * 用于构建层级分类结构，null表示顶级分类
     */
    @Column(name = "parent_id")
    private Long parentId;

    /**
     * 分类层级
     * 从0开始，0表示顶级分类，1表示二级分类，以此类推
     */
    @Column(nullable = false)
    private Integer level = 0;

    /**
     * 分类路径
     * 存储从根分类到当前分类的完整路径，格式如：/1/2/3
     */
    @Column(name = "category_path", length = 1000)
    private String categoryPath;

    /**
     * 排序权重
     * 用于控制同级分类的显示顺序，数值越小越靠前
     */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    /**
     * 分类状态
     * ACTIVE: 激活状态，可正常使用
     * INACTIVE: 非激活状态，暂时禁用
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryStatus status = CategoryStatus.ACTIVE;

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
     * 创建者
     */
    @Column(name = "created_by", length = 100)
    private String createdBy;

    /**
     * 子分类列表
     * 一对多关系，一个分类可以有多个子分类
     */
    @OneToMany(mappedBy = "parentId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<KnowledgeCategory> children = new ArrayList<>();

    /**
     * 该分类下的知识文档列表
     * 一对多关系，一个分类可以包含多个知识文档
     */
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<KnowledgeDocument> documents = new ArrayList<>();

    /**
     * 分类状态枚举
     */
    public enum CategoryStatus {
        /**
         * 激活状态 - 分类可正常使用
         */
        ACTIVE,
        
        /**
         * 非激活状态 - 分类暂时禁用
         */
        INACTIVE
    }

    /**
     * 默认构造函数
     */
    public KnowledgeCategory() {
    }

    /**
     * 构造函数
     * 
     * @param name 分类名称
     * @param description 分类描述
     * @param parentId 父分类ID
     */
    public KnowledgeCategory(String name, String description, Long parentId) {
        this.name = name;
        this.description = description;
        this.parentId = parentId;
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
     * 判断是否为顶级分类
     * 
     * @return true如果是顶级分类，false否则
     */
    public boolean isTopLevel() {
        return this.parentId == null;
    }

    /**
     * 判断是否有子分类
     * 
     * @return true如果有子分类，false否则
     */
    public boolean hasChildren() {
        return this.children != null && !this.children.isEmpty();
    }

    /**
     * 添加子分类
     * 
     * @param child 子分类
     */
    public void addChild(KnowledgeCategory child) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(child);
        child.setParentId(this.id);
        child.setLevel(this.level + 1);
    }

    /**
     * 添加知识文档
     * 
     * @param document 知识文档
     */
    public void addDocument(KnowledgeDocument document) {
        if (this.documents == null) {
            this.documents = new ArrayList<>();
        }
        this.documents.add(document);
        document.setCategory(this);
    }

    // Getter和Setter方法

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getCategoryPath() {
        return categoryPath;
    }

    public void setCategoryPath(String categoryPath) {
        this.categoryPath = categoryPath;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public CategoryStatus getStatus() {
        return status;
    }

    public void setStatus(CategoryStatus status) {
        this.status = status;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public List<KnowledgeCategory> getChildren() {
        return children;
    }

    public void setChildren(List<KnowledgeCategory> children) {
        this.children = children;
    }

    public List<KnowledgeDocument> getDocuments() {
        return documents;
    }

    public void setDocuments(List<KnowledgeDocument> documents) {
        this.documents = documents;
    }

    @Override
    public String toString() {
        return "KnowledgeCategory{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", parentId=" + parentId +
                ", level=" + level +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}
