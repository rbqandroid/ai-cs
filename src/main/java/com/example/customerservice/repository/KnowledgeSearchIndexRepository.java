package com.example.customerservice.repository;

import com.example.customerservice.entity.KnowledgeSearchIndex;
import com.example.customerservice.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 知识库搜索索引数据访问层接口
 * 
 * 提供搜索索引的数据库操作方法，包括索引的创建、更新、查询和维护功能。
 * 支持关键词索引和向量化索引的管理。
 * 
 * @author AI Assistant
 * @version 1.0.0
 * @since 2025-07-06
 */
@Repository
public interface KnowledgeSearchIndexRepository extends JpaRepository<KnowledgeSearchIndex, Long> {

    /**
     * 根据文档查找搜索索引
     * 
     * @param document 知识文档
     * @return 搜索索引对象，如果不存在则返回空
     */
    Optional<KnowledgeSearchIndex> findByDocument(KnowledgeDocument document);

    /**
     * 根据文档ID查找搜索索引
     * 
     * @param documentId 文档ID
     * @return 搜索索引对象，如果不存在则返回空
     */
    @Query("SELECT si FROM KnowledgeSearchIndex si WHERE si.document.id = :documentId")
    Optional<KnowledgeSearchIndex> findByDocumentId(@Param("documentId") Long documentId);

    /**
     * 查找所有就绪状态的搜索索引
     * 
     * @return 就绪状态的索引列表
     */
    List<KnowledgeSearchIndex> findByStatus(KnowledgeSearchIndex.IndexStatus status);

    /**
     * 查找需要重建的索引
     * 包括状态为ERROR或BUILDING超过指定时间的索引
     * 
     * @param timeThreshold 时间阈值，超过此时间的BUILDING状态索引将被重建
     * @return 需要重建的索引列表
     */
    @Query("SELECT si FROM KnowledgeSearchIndex si WHERE " +
           "si.status = 'ERROR' OR " +
           "(si.status = 'BUILDING' AND si.updatedAt < :timeThreshold)")
    List<KnowledgeSearchIndex> findIndexesNeedingRebuild(@Param("timeThreshold") LocalDateTime timeThreshold);

    /**
     * 查找包含指定关键词的索引
     * 用于关键词搜索
     * 
     * @param keyword 搜索关键词
     * @return 匹配的索引列表
     */
    @Query("SELECT si FROM KnowledgeSearchIndex si WHERE " +
           "si.status = 'READY' AND " +
           "LOWER(si.keywords) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<KnowledgeSearchIndex> findByKeywordsContaining(@Param("keyword") String keyword);

    /**
     * 查找有向量化表示的索引
     * 用于语义搜索
     * 
     * @return 有向量化表示的索引列表
     */
    @Query("SELECT si FROM KnowledgeSearchIndex si WHERE " +
           "si.status = 'READY' AND " +
           "si.embedding IS NOT NULL AND " +
           "si.embeddingDimension IS NOT NULL")
    List<KnowledgeSearchIndex> findIndexesWithEmbedding();

    /**
     * 统计各状态的索引数量
     * 
     * @return 状态统计结果
     */
    @Query("SELECT si.status, COUNT(si) FROM KnowledgeSearchIndex si GROUP BY si.status")
    List<Object[]> countByStatus();

    /**
     * 统计指定向量维度的索引数量
     * 
     * @param dimension 向量维度
     * @return 索引数量
     */
    long countByEmbeddingDimension(Integer dimension);

    /**
     * 更新索引状态
     * 
     * @param indexId 索引ID
     * @param status 新状态
     * @param errorMessage 错误信息（可为null）
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE KnowledgeSearchIndex si SET " +
           "si.status = :status, " +
           "si.errorMessage = :errorMessage, " +
           "si.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE si.id = :indexId")
    int updateStatus(@Param("indexId") Long indexId, 
                    @Param("status") KnowledgeSearchIndex.IndexStatus status,
                    @Param("errorMessage") String errorMessage);

    /**
     * 更新索引的关键词
     * 
     * @param indexId 索引ID
     * @param keywords 新的关键词
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE KnowledgeSearchIndex si SET " +
           "si.keywords = :keywords, " +
           "si.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE si.id = :indexId")
    int updateKeywords(@Param("indexId") Long indexId, @Param("keywords") String keywords);

    /**
     * 更新索引的向量化表示
     * 
     * @param indexId 索引ID
     * @param embedding 向量化表示
     * @param dimension 向量维度
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE KnowledgeSearchIndex si SET " +
           "si.embedding = :embedding, " +
           "si.embeddingDimension = :dimension, " +
           "si.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE si.id = :indexId")
    int updateEmbedding(@Param("indexId") Long indexId, 
                       @Param("embedding") String embedding,
                       @Param("dimension") Integer dimension);

    /**
     * 批量删除指定文档的索引
     * 
     * @param documentIds 文档ID列表
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM KnowledgeSearchIndex si WHERE si.document.id IN :documentIds")
    int deleteByDocumentIds(@Param("documentIds") List<Long> documentIds);

    /**
     * 删除过期的错误索引
     * 删除状态为ERROR且超过指定时间的索引
     * 
     * @param timeThreshold 时间阈值
     * @return 删除的记录数
     */
    @Modifying
    @Query("DELETE FROM KnowledgeSearchIndex si WHERE " +
           "si.status = 'ERROR' AND si.updatedAt < :timeThreshold")
    int deleteExpiredErrorIndexes(@Param("timeThreshold") LocalDateTime timeThreshold);

    /**
     * 查找最近更新的索引
     * 用于监控和调试
     * 
     * @param limit 返回记录数限制
     * @return 最近更新的索引列表
     */
    @Query("SELECT si FROM KnowledgeSearchIndex si ORDER BY si.updatedAt DESC")
    List<KnowledgeSearchIndex> findRecentlyUpdated(org.springframework.data.domain.Pageable pageable);

    /**
     * 获取索引统计信息
     * 
     * @return 包含各种统计数据的对象数组
     */
    @Query("SELECT " +
           "COUNT(si) as totalCount, " +
           "COUNT(CASE WHEN si.status = 'READY' THEN 1 END) as readyCount, " +
           "COUNT(CASE WHEN si.status = 'BUILDING' THEN 1 END) as buildingCount, " +
           "COUNT(CASE WHEN si.status = 'ERROR' THEN 1 END) as errorCount, " +
           "COUNT(CASE WHEN si.embedding IS NOT NULL THEN 1 END) as embeddingCount, " +
           "AVG(si.embeddingDimension) as avgDimension " +
           "FROM KnowledgeSearchIndex si")
    Object[] getIndexStatistics();

    /**
     * 查找指定文档的相关索引
     * 基于关键词相似性查找
     *
     * @param excludeDocumentId 排除的文档ID
     * @param pageable 分页参数
     * @return 相关索引列表
     */
    @Query("SELECT si FROM KnowledgeSearchIndex si WHERE " +
           "si.status = 'READY' AND " +
           "si.document.id != :excludeDocumentId AND " +
           "si.document.status = 'PUBLISHED' " +
           "ORDER BY si.document.viewCount DESC")
    List<KnowledgeSearchIndex> findRelatedIndexes(@Param("excludeDocumentId") Long excludeDocumentId,
                                                  org.springframework.data.domain.Pageable pageable);

    /**
     * 检查索引是否存在
     * 
     * @param documentId 文档ID
     * @return 是否存在索引
     */
    @Query("SELECT COUNT(si) > 0 FROM KnowledgeSearchIndex si WHERE si.document.id = :documentId")
    boolean existsByDocumentId(@Param("documentId") Long documentId);
}
