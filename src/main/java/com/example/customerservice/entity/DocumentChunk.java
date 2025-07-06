package com.example.customerservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 文档分块实体类
 * 
 * 用于存储长文档分割后的片段，每个片段包含原始内容、向量表示和元数据。
 * 支持RAG功能中的细粒度文档检索和相似度匹配。
 * 
 * @author AI Assistant
 * @version 1.0.0
 * @since 2025-07-06
 */
@Entity
@Table(name = "document_chunks", indexes = {
    @Index(name = "idx_document_id", columnList = "document_id"),
    @Index(name = "idx_chunk_index", columnList = "chunk_index"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class DocumentChunk {

    /**
     * 分块唯一标识符
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的知识文档
     * 多对一关系，一个文档可以有多个分块
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private KnowledgeDocument document;

    /**
     * 分块在文档中的索引位置
     * 从0开始，用于保持分块的顺序
     */
    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    /**
     * 分块内容
     * 存储文档片段的实际文本内容
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 分块摘要
     * 可选字段，用于快速预览分块内容
     */
    @Column(length = 500)
    private String summary;

    /**
     * 分块大小（字符数）
     * 记录分块的字符数量，用于统计和优化
     */
    @Column(name = "chunk_size", nullable = false)
    private Integer chunkSize;

    /**
     * 向量表示
     * 存储分块内容的向量化表示，用于相似度搜索
     * 格式为JSON数组字符串，如：[0.1, 0.2, 0.3, ...]
     */
    @Column(columnDefinition = "TEXT")
    private String embedding;

    /**
     * 向量维度
     * 记录向量的维度大小
     */
    @Column(name = "embedding_dimension")
    private Integer embeddingDimension;

    /**
     * 开始位置
     * 分块在原文档中的起始字符位置
     */
    @Column(name = "start_position")
    private Integer startPosition;

    /**
     * 结束位置
     * 分块在原文档中的结束字符位置
     */
    @Column(name = "end_position")
    private Integer endPosition;

    /**
     * 重叠长度
     * 与前一个分块的重叠字符数，用于保持上下文连续性
     */
    @Column(name = "overlap_length")
    private Integer overlapLength = 0;

    /**
     * 分块状态
     * PENDING: 待处理
     * PROCESSING: 正在处理
     * READY: 已就绪，可用于搜索
     * ERROR: 处理失败
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChunkStatus status = ChunkStatus.PENDING;

    /**
     * 错误信息
     * 当处理失败时记录错误详情
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
     * 分块状态枚举
     */
    public enum ChunkStatus {
        /**
         * 待处理 - 分块已创建但未开始向量化
         */
        PENDING,
        
        /**
         * 正在处理 - 分块正在进行向量化处理
         */
        PROCESSING,
        
        /**
         * 已就绪 - 分块已完成向量化，可用于搜索
         */
        READY,
        
        /**
         * 处理失败 - 向量化处理失败
         */
        ERROR
    }

    /**
     * 默认构造函数
     */
    public DocumentChunk() {
    }

    /**
     * 构造函数
     * 
     * @param document 关联的知识文档
     * @param chunkIndex 分块索引
     * @param content 分块内容
     */
    public DocumentChunk(KnowledgeDocument document, Integer chunkIndex, String content) {
        this.document = document;
        this.chunkIndex = chunkIndex;
        this.content = content;
        this.chunkSize = content != null ? content.length() : 0;
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
     * 标记为处理中状态
     */
    public void markAsProcessing() {
        this.status = ChunkStatus.PROCESSING;
        this.errorMessage = null;
    }

    /**
     * 标记为就绪状态
     */
    public void markAsReady() {
        this.status = ChunkStatus.READY;
        this.errorMessage = null;
    }

    /**
     * 标记为错误状态
     * 
     * @param errorMessage 错误信息
     */
    public void markAsError(String errorMessage) {
        this.status = ChunkStatus.ERROR;
        this.errorMessage = errorMessage;
    }

    /**
     * 判断是否已就绪
     * 
     * @return true如果已就绪，false否则
     */
    public boolean isReady() {
        return ChunkStatus.READY.equals(this.status);
    }

    /**
     * 判断是否正在处理
     * 
     * @return true如果正在处理，false否则
     */
    public boolean isProcessing() {
        return ChunkStatus.PROCESSING.equals(this.status);
    }

    /**
     * 判断是否有错误
     * 
     * @return true如果有错误，false否则
     */
    public boolean hasError() {
        return ChunkStatus.ERROR.equals(this.status);
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

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.chunkSize = content != null ? content.length() : 0;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Integer getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Integer chunkSize) {
        this.chunkSize = chunkSize;
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

    public Integer getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(Integer startPosition) {
        this.startPosition = startPosition;
    }

    public Integer getEndPosition() {
        return endPosition;
    }

    public void setEndPosition(Integer endPosition) {
        this.endPosition = endPosition;
    }

    public Integer getOverlapLength() {
        return overlapLength;
    }

    public void setOverlapLength(Integer overlapLength) {
        this.overlapLength = overlapLength;
    }

    public ChunkStatus getStatus() {
        return status;
    }

    public void setStatus(ChunkStatus status) {
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
        return "DocumentChunk{" +
                "id=" + id +
                ", documentId=" + (document != null ? document.getId() : null) +
                ", chunkIndex=" + chunkIndex +
                ", chunkSize=" + chunkSize +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}
