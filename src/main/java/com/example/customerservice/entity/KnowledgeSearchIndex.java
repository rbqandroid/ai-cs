package com.example.customerservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 知识库搜索索引实体类
 * 
 * 用于存储知识文档的搜索索引信息，包括关键词索引和向量化表示。
 * 支持传统的关键词搜索和基于AI的语义搜索功能。
 * 
 * @author AI Assistant
 * @version 1.0.0
 * @since 2025-07-06
 */
@Entity
@Table(name = "knowledge_search_indexes", indexes = {
    @Index(name = "idx_document_id", columnList = "document_id"),
    @Index(name = "idx_updated_at", columnList = "updated_at")
})
public class KnowledgeSearchIndex {

    /**
     * 索引唯一标识符
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的知识文档
     * 一对一关系，每个文档对应一个搜索索引
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false, unique = true)
    private KnowledgeDocument document;

    /**
     * 关键词索引
     * 存储从文档标题和内容中提取的关键词，用于传统的关键词搜索
     */
    @Column(columnDefinition = "TEXT")
    private String keywords;

    /**
     * 向量化表示
     * 存储文档的向量化表示（Embedding），用于语义搜索
     * 格式为JSON数组字符串，如：[0.1, 0.2, 0.3, ...]
     */
    @Column(columnDefinition = "TEXT")
    private String embedding;

    /**
     * 向量维度
     * 记录向量的维度大小，便于验证和处理
     */
    @Column(name = "embedding_dimension")
    private Integer embeddingDimension;

    /**
     * 索引版本
     * 用于跟踪索引的版本，当文档更新时递增
     */
    @Column(nullable = false)
    private Integer version = 1;

    /**
     * 索引状态
     * BUILDING: 正在构建索引
     * READY: 索引已就绪，可用于搜索
     * ERROR: 索引构建失败
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IndexStatus status = IndexStatus.BUILDING;

    /**
     * 错误信息
     * 当索引构建失败时，记录具体的错误信息
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

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
     * 索引状态枚举
     */
    public enum IndexStatus {
        /**
         * 正在构建索引
         */
        BUILDING,
        
        /**
         * 索引已就绪，可用于搜索
         */
        READY,
        
        /**
         * 索引构建失败
         */
        ERROR
    }

    /**
     * 默认构造函数
     */
    public KnowledgeSearchIndex() {
    }

    /**
     * 构造函数
     * 
     * @param document 关联的知识文档
     */
    public KnowledgeSearchIndex(KnowledgeDocument document) {
        this.document = document;
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
     * 标记索引为就绪状态
     */
    public void markAsReady() {
        this.status = IndexStatus.READY;
        this.errorMessage = null;
    }

    /**
     * 标记索引为错误状态
     * 
     * @param errorMessage 错误信息
     */
    public void markAsError(String errorMessage) {
        this.status = IndexStatus.ERROR;
        this.errorMessage = errorMessage;
    }

    /**
     * 开始构建索引
     */
    public void startBuilding() {
        this.status = IndexStatus.BUILDING;
        this.errorMessage = null;
        this.version++;
    }

    /**
     * 判断索引是否就绪
     * 
     * @return true如果索引就绪，false否则
     */
    public boolean isReady() {
        return IndexStatus.READY.equals(this.status);
    }

    /**
     * 判断索引是否正在构建
     * 
     * @return true如果正在构建，false否则
     */
    public boolean isBuilding() {
        return IndexStatus.BUILDING.equals(this.status);
    }

    /**
     * 判断索引是否有错误
     * 
     * @return true如果有错误，false否则
     */
    public boolean hasError() {
        return IndexStatus.ERROR.equals(this.status);
    }

    /**
     * 获取向量化表示的浮点数组
     * 
     * @return 向量数组，如果解析失败返回null
     */
    public float[] getEmbeddingArray() {
        if (embedding == null || embedding.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 移除方括号并按逗号分割
            String cleanEmbedding = embedding.trim().replaceAll("^\\[|\\]$", "");
            String[] parts = cleanEmbedding.split(",");
            float[] result = new float[parts.length];
            
            for (int i = 0; i < parts.length; i++) {
                result[i] = Float.parseFloat(parts[i].trim());
            }
            
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 设置向量化表示
     * 
     * @param embeddingArray 向量数组
     */
    public void setEmbeddingArray(float[] embeddingArray) {
        if (embeddingArray == null || embeddingArray.length == 0) {
            this.embedding = null;
            this.embeddingDimension = null;
            return;
        }
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embeddingArray.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(embeddingArray[i]);
        }
        sb.append("]");
        
        this.embedding = sb.toString();
        this.embeddingDimension = embeddingArray.length;
    }

    // Getter和Setter方法

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public KnowledgeDocument getDocument() {
        return document;
    }

    public void setDocument(KnowledgeDocument document) {
        this.document = document;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getEmbedding() {
        return embedding;
    }

    public void setEmbedding(String embedding) {
        this.embedding = embedding;
    }

    public Integer getEmbeddingDimension() {
        return embeddingDimension;
    }

    public void setEmbeddingDimension(Integer embeddingDimension) {
        this.embeddingDimension = embeddingDimension;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public IndexStatus getStatus() {
        return status;
    }

    public void setStatus(IndexStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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

    @Override
    public String toString() {
        return "KnowledgeSearchIndex{" +
                "id=" + id +
                ", documentId=" + (document != null ? document.getId() : null) +
                ", status=" + status +
                ", version=" + version +
                ", embeddingDimension=" + embeddingDimension +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
