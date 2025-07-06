package com.example.customerservice.repository;

import com.example.customerservice.entity.KnowledgeDocument;
import com.example.customerservice.entity.KnowledgeCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 知识文档数据访问层接口
 * 
 * 提供知识文档的数据库操作方法，包括基本的CRUD操作、搜索功能和统计查询。
 * 支持全文搜索、分类查询、状态过滤等功能。
 * 
 * @author AI Assistant
 * @version 1.0.0
 * @since 2025-07-06
 */
@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {

    /**
     * 根据文档标题查找文档
     * 
     * @param title 文档标题
     * @return 文档对象，如果不存在则返回空
     */
    Optional<KnowledgeDocument> findByTitle(String title);

    /**
     * 根据分类查找已发布的文档
     * 按优先级降序、创建时间降序排列
     * 
     * @param category 分类对象
     * @param pageable 分页参数
     * @return 文档分页结果
     */
    Page<KnowledgeDocument> findByCategoryAndStatusOrderByPriorityDescCreatedAtDesc(
            KnowledgeCategory category, 
            KnowledgeDocument.DocumentStatus status, 
            Pageable pageable);

    /**
     * 根据文档状态查找文档
     *
     * @param status 文档状态
     * @return 文档列表
     */
    List<KnowledgeDocument> findByStatus(KnowledgeDocument.DocumentStatus status);

    /**
     * 根据文档状态查找文档（分页）
     *
     * @param status 文档状态
     * @param pageable 分页参数
     * @return 文档分页结果
     */
    Page<KnowledgeDocument> findByStatusOrderByUpdatedAtDesc(
            KnowledgeDocument.DocumentStatus status,
            Pageable pageable);

    /**
     * 根据创建者查找文档
     * 
     * @param createdBy 创建者
     * @param pageable 分页参数
     * @return 文档分页结果
     */
    Page<KnowledgeDocument> findByCreatedByOrderByCreatedAtDesc(String createdBy, Pageable pageable);

    /**
     * 在文档标题中搜索关键词
     * 
     * @param keyword 搜索关键词
     * @param status 文档状态
     * @param pageable 分页参数
     * @return 匹配的文档分页结果
     */
    Page<KnowledgeDocument> findByTitleContainingIgnoreCaseAndStatusOrderByPriorityDescViewCountDesc(
            String keyword, 
            KnowledgeDocument.DocumentStatus status, 
            Pageable pageable);

    /**
     * 全文搜索文档
     * 在标题、内容和摘要中搜索关键词
     * 
     * @param keyword 搜索关键词
     * @param status 文档状态
     * @param pageable 分页参数
     * @return 匹配的文档分页结果
     */
    @Query("SELECT d FROM KnowledgeDocument d WHERE " +
           "d.status = :status AND (" +
           "LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(d.summary) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
           ") ORDER BY d.priority DESC, d.viewCount DESC")
    Page<KnowledgeDocument> searchByKeyword(@Param("keyword") String keyword, 
                                           @Param("status") KnowledgeDocument.DocumentStatus status, 
                                           Pageable pageable);

    /**
     * 根据标签搜索文档
     * 
     * @param tag 标签
     * @param status 文档状态
     * @param pageable 分页参数
     * @return 匹配的文档分页结果
     */
    @Query("SELECT d FROM KnowledgeDocument d WHERE " +
           "d.status = :status AND " +
           "LOWER(d.tags) LIKE LOWER(CONCAT('%', :tag, '%')) " +
           "ORDER BY d.priority DESC, d.viewCount DESC")
    Page<KnowledgeDocument> findByTagAndStatus(@Param("tag") String tag, 
                                              @Param("status") KnowledgeDocument.DocumentStatus status, 
                                              Pageable pageable);

    /**
     * 查找热门文档
     * 根据访问次数排序
     * 
     * @param status 文档状态
     * @param pageable 分页参数
     * @return 热门文档分页结果
     */
    Page<KnowledgeDocument> findByStatusOrderByViewCountDescCreatedAtDesc(
            KnowledgeDocument.DocumentStatus status, 
            Pageable pageable);

    /**
     * 查找最新文档
     * 根据发布时间排序
     * 
     * @param status 文档状态
     * @param pageable 分页参数
     * @return 最新文档分页结果
     */
    Page<KnowledgeDocument> findByStatusOrderByPublishedAtDescCreatedAtDesc(
            KnowledgeDocument.DocumentStatus status, 
            Pageable pageable);

    /**
     * 查找指定时间范围内创建的文档
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param status 文档状态
     * @return 文档列表
     */
    List<KnowledgeDocument> findByCreatedAtBetweenAndStatus(
            LocalDateTime startTime, 
            LocalDateTime endTime, 
            KnowledgeDocument.DocumentStatus status);

    /**
     * 统计指定分类下的文档数量
     * 
     * @param category 分类对象
     * @param status 文档状态
     * @return 文档数量
     */
    long countByCategoryAndStatus(KnowledgeCategory category, KnowledgeDocument.DocumentStatus status);

    /**
     * 统计指定状态的文档数量
     * 
     * @param status 文档状态
     * @return 文档数量
     */
    long countByStatus(KnowledgeDocument.DocumentStatus status);

    /**
     * 统计指定创建者的文档数量
     * 
     * @param createdBy 创建者
     * @return 文档数量
     */
    long countByCreatedBy(String createdBy);

    /**
     * 增加文档访问次数
     * 
     * @param documentId 文档ID
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE KnowledgeDocument d SET d.viewCount = d.viewCount + 1 WHERE d.id = :documentId")
    int incrementViewCount(@Param("documentId") Long documentId);

    /**
     * 增加文档点赞次数
     * 
     * @param documentId 文档ID
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE KnowledgeDocument d SET d.likeCount = d.likeCount + 1 WHERE d.id = :documentId")
    int incrementLikeCount(@Param("documentId") Long documentId);

    /**
     * 批量更新文档状态
     * 
     * @param documentIds 文档ID列表
     * @param status 新状态
     * @param updatedBy 更新者
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE KnowledgeDocument d SET d.status = :status, d.updatedBy = :updatedBy, d.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE d.id IN :documentIds")
    int updateStatusByIds(@Param("documentIds") List<Long> documentIds, 
                         @Param("status") KnowledgeDocument.DocumentStatus status,
                         @Param("updatedBy") String updatedBy);

    /**
     * 查找需要重建索引的文档
     * 查找文档更新时间晚于索引更新时间的文档
     * 
     * @return 需要重建索引的文档列表
     */
    @Query("SELECT d FROM KnowledgeDocument d LEFT JOIN d.searchIndex si " +
           "WHERE d.status = 'PUBLISHED' AND " +
           "(si IS NULL OR d.updatedAt > si.updatedAt)")
    List<KnowledgeDocument> findDocumentsNeedingIndexUpdate();

    /**
     * 查找相似文档
     * 根据标签和分类查找相似文档
     *
     * @param documentId 当前文档ID
     * @param categoryId 分类ID
     * @param pageable 分页参数
     * @return 相似文档分页结果
     */
    @Query("SELECT d FROM KnowledgeDocument d WHERE " +
           "d.id != :documentId AND d.status = 'PUBLISHED' AND " +
           "d.category.id = :categoryId " +
           "ORDER BY d.viewCount DESC, d.likeCount DESC")
    Page<KnowledgeDocument> findSimilarDocuments(@Param("documentId") Long documentId,
                                                @Param("categoryId") Long categoryId,
                                                Pageable pageable);

    /**
     * 获取文档统计信息
     * 
     * @return 包含各种统计数据的对象数组
     */
    @Query("SELECT " +
           "COUNT(d) as totalCount, " +
           "COUNT(CASE WHEN d.status = 'PUBLISHED' THEN 1 END) as publishedCount, " +
           "COUNT(CASE WHEN d.status = 'DRAFT' THEN 1 END) as draftCount, " +
           "COUNT(CASE WHEN d.status = 'ARCHIVED' THEN 1 END) as archivedCount, " +
           "SUM(d.viewCount) as totalViews, " +
           "SUM(d.likeCount) as totalLikes " +
           "FROM KnowledgeDocument d")
    Object[] getDocumentStatistics();
}
