package com.example.customerservice.repository;

import com.example.customerservice.entity.DocumentChunk;
import com.example.customerservice.entity.KnowledgeDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档分块数据访问层接口
 * 
 * 提供文档分块的数据库操作方法，包括基本的CRUD操作、状态管理和向量搜索支持。
 * 支持RAG功能中的文档片段检索和相似度匹配。
 * 
 * @author AI Assistant
 * @version 1.0.0
 * @since 2025-07-06
 */
@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    /**
     * 根据文档查找所有分块
     * 按分块索引升序排列
     * 
     * @param document 知识文档
     * @return 分块列表
     */
    List<DocumentChunk> findByDocumentOrderByChunkIndexAsc(KnowledgeDocument document);

    /**
     * 根据文档ID查找所有分块
     * 
     * @param documentId 文档ID
     * @return 分块列表
     */
    @Query("SELECT dc FROM DocumentChunk dc WHERE dc.document.id = :documentId ORDER BY dc.chunkIndex ASC")
    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(@Param("documentId") Long documentId);

    /**
     * 根据状态查找分块
     * 
     * @param status 分块状态
     * @return 分块列表
     */
    List<DocumentChunk> findByStatus(DocumentChunk.ChunkStatus status);

    /**
     * 根据状态查找分块（分页）
     * 
     * @param status 分块状态
     * @param pageable 分页参数
     * @return 分块分页结果
     */
    Page<DocumentChunk> findByStatusOrderByCreatedAtDesc(DocumentChunk.ChunkStatus status, Pageable pageable);

    /**
     * 查找已就绪的分块（有向量表示）
     * 
     * @return 已就绪的分块列表
     */
    @Query("SELECT dc FROM DocumentChunk dc WHERE dc.status = 'READY' AND dc.embedding IS NOT NULL")
    List<DocumentChunk> findReadyChunksWithEmbedding();

    /**
     * 查找已就绪的分块（分页）
     * 
     * @param pageable 分页参数
     * @return 已就绪的分块分页结果
     */
    @Query("SELECT dc FROM DocumentChunk dc WHERE dc.status = 'READY' AND dc.embedding IS NOT NULL ORDER BY dc.createdAt DESC")
    Page<DocumentChunk> findReadyChunksWithEmbedding(Pageable pageable);

    /**
     * 查找需要处理的分块
     * 包括状态为PENDING或处理超时的PROCESSING状态分块
     * 
     * @param timeoutThreshold 超时时间阈值
     * @return 需要处理的分块列表
     */
    @Query("SELECT dc FROM DocumentChunk dc WHERE " +
           "dc.status = 'PENDING' OR " +
           "(dc.status = 'PROCESSING' AND dc.updatedAt < :timeoutThreshold)")
    List<DocumentChunk> findChunksNeedingProcessing(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);

    /**
     * 根据文档和分块索引查找分块
     * 
     * @param document 知识文档
     * @param chunkIndex 分块索引
     * @return 分块对象
     */
    DocumentChunk findByDocumentAndChunkIndex(KnowledgeDocument document, Integer chunkIndex);

    /**
     * 统计文档的分块数量
     * 
     * @param document 知识文档
     * @return 分块数量
     */
    long countByDocument(KnowledgeDocument document);

    /**
     * 统计指定状态的分块数量
     * 
     * @param status 分块状态
     * @return 分块数量
     */
    long countByStatus(DocumentChunk.ChunkStatus status);

    /**
     * 统计文档中指定状态的分块数量
     * 
     * @param document 知识文档
     * @param status 分块状态
     * @return 分块数量
     */
    long countByDocumentAndStatus(KnowledgeDocument document, DocumentChunk.ChunkStatus status);

    /**
     * 删除文档的所有分块
     * 
     * @param document 知识文档
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM DocumentChunk dc WHERE dc.document = :document")
    int deleteByDocument(@Param("document") KnowledgeDocument document);

    /**
     * 删除指定文档ID的所有分块
     * 
     * @param documentId 文档ID
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM DocumentChunk dc WHERE dc.document.id = :documentId")
    int deleteByDocumentId(@Param("documentId") Long documentId);

    /**
     * 批量更新分块状态
     * 
     * @param chunkIds 分块ID列表
     * @param status 新状态
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE DocumentChunk dc SET dc.status = :status, dc.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE dc.id IN :chunkIds")
    int updateStatusByIds(@Param("chunkIds") List<Long> chunkIds, 
                         @Param("status") DocumentChunk.ChunkStatus status);

    /**
     * 更新分块的向量表示
     * 
     * @param chunkId 分块ID
     * @param embedding 向量表示
     * @param dimension 向量维度
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE DocumentChunk dc SET " +
           "dc.embedding = :embedding, " +
           "dc.embeddingDimension = :dimension, " +
           "dc.status = 'READY', " +
           "dc.errorMessage = null, " +
           "dc.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE dc.id = :chunkId")
    int updateEmbedding(@Param("chunkId") Long chunkId, 
                       @Param("embedding") String embedding,
                       @Param("dimension") Integer dimension);

    /**
     * 标记分块处理失败
     * 
     * @param chunkId 分块ID
     * @param errorMessage 错误信息
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE DocumentChunk dc SET " +
           "dc.status = 'ERROR', " +
           "dc.errorMessage = :errorMessage, " +
           "dc.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE dc.id = :chunkId")
    int markAsError(@Param("chunkId") Long chunkId, @Param("errorMessage") String errorMessage);

    /**
     * 查找指定大小范围内的分块
     * 
     * @param minSize 最小大小
     * @param maxSize 最大大小
     * @return 分块列表
     */
    @Query("SELECT dc FROM DocumentChunk dc WHERE dc.chunkSize BETWEEN :minSize AND :maxSize")
    List<DocumentChunk> findByChunkSizeBetween(@Param("minSize") Integer minSize, 
                                              @Param("maxSize") Integer maxSize);

    /**
     * 查找最近创建的分块
     * 
     * @param pageable 分页参数
     * @return 最近创建的分块分页结果
     */
    Page<DocumentChunk> findByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 查找指定时间范围内创建的分块
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 分块列表
     */
    List<DocumentChunk> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取分块统计信息
     * 
     * @return 包含各种统计数据的对象数组
     */
    @Query("SELECT " +
           "COUNT(dc) as totalCount, " +
           "COUNT(CASE WHEN dc.status = 'READY' THEN 1 END) as readyCount, " +
           "COUNT(CASE WHEN dc.status = 'PENDING' THEN 1 END) as pendingCount, " +
           "COUNT(CASE WHEN dc.status = 'PROCESSING' THEN 1 END) as processingCount, " +
           "COUNT(CASE WHEN dc.status = 'ERROR' THEN 1 END) as errorCount, " +
           "COUNT(CASE WHEN dc.embedding IS NOT NULL THEN 1 END) as embeddingCount, " +
           "AVG(dc.chunkSize) as avgChunkSize, " +
           "AVG(dc.embeddingDimension) as avgDimension " +
           "FROM DocumentChunk dc")
    Object[] getChunkStatistics();

    /**
     * 查找相似大小的分块
     * 用于优化分块策略
     * 
     * @param targetSize 目标大小
     * @param tolerance 容差范围
     * @param pageable 分页参数
     * @return 相似大小的分块分页结果
     */
    @Query("SELECT dc FROM DocumentChunk dc WHERE " +
           "ABS(dc.chunkSize - :targetSize) <= :tolerance " +
           "ORDER BY ABS(dc.chunkSize - :targetSize) ASC")
    Page<DocumentChunk> findSimilarSizedChunks(@Param("targetSize") Integer targetSize,
                                              @Param("tolerance") Integer tolerance,
                                              Pageable pageable);

    /**
     * 查找文档中的相邻分块
     * 
     * @param document 知识文档
     * @param chunkIndex 当前分块索引
     * @param range 范围（前后各多少个分块）
     * @return 相邻分块列表
     */
    @Query("SELECT dc FROM DocumentChunk dc WHERE " +
           "dc.document = :document AND " +
           "dc.chunkIndex BETWEEN :startIndex AND :endIndex " +
           "ORDER BY dc.chunkIndex ASC")
    List<DocumentChunk> findAdjacentChunks(@Param("document") KnowledgeDocument document,
                                          @Param("startIndex") Integer startIndex,
                                          @Param("endIndex") Integer endIndex);

    /**
     * 检查分块是否存在
     * 
     * @param document 知识文档
     * @param chunkIndex 分块索引
     * @return 是否存在
     */
    boolean existsByDocumentAndChunkIndex(KnowledgeDocument document, Integer chunkIndex);

    /**
     * 获取文档的最大分块索引
     * 
     * @param document 知识文档
     * @return 最大分块索引，如果没有分块返回null
     */
    @Query("SELECT MAX(dc.chunkIndex) FROM DocumentChunk dc WHERE dc.document = :document")
    Integer findMaxChunkIndexByDocument(@Param("document") KnowledgeDocument document);

    /**
     * 清理过期的错误分块
     * 删除状态为ERROR且超过指定时间的分块
     * 
     * @param timeThreshold 时间阈值
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM DocumentChunk dc WHERE " +
           "dc.status = 'ERROR' AND dc.updatedAt < :timeThreshold")
    int deleteExpiredErrorChunks(@Param("timeThreshold") LocalDateTime timeThreshold);
}
