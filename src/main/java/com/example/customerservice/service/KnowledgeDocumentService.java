package com.example.customerservice.service;

import com.example.customerservice.entity.KnowledgeDocument;
import com.example.customerservice.entity.KnowledgeCategory;
import com.example.customerservice.entity.KnowledgeSearchIndex;
import com.example.customerservice.repository.KnowledgeDocumentRepository;
import com.example.customerservice.repository.KnowledgeCategoryRepository;
import com.example.customerservice.repository.KnowledgeSearchIndexRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 知识文档服务类
 * 
 * 提供知识文档的业务逻辑处理，包括文档的创建、更新、删除、查询、搜索等功能。
 * 支持文档状态管理、访问统计和搜索索引的自动维护。
 * 
 * @author AI Assistant
 * @version 1.0.0
 * @since 2025-07-06
 */
@Service
@Transactional
public class KnowledgeDocumentService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeDocumentService.class);

    @Autowired
    private KnowledgeDocumentRepository documentRepository;

    @Autowired
    private KnowledgeCategoryRepository categoryRepository;

    @Autowired
    private KnowledgeSearchIndexRepository searchIndexRepository;

    @Autowired
    private KnowledgeSearchService searchService;

    @Autowired
    private EmbeddingService embeddingService;

    /**
     * 创建新的知识文档
     * 
     * @param document 文档对象
     * @param createdBy 创建者
     * @return 创建的文档对象
     * @throws IllegalArgumentException 当文档信息无效时抛出
     */
    public KnowledgeDocument createDocument(KnowledgeDocument document, String createdBy) {
        logger.info("创建知识文档: {}, 创建者: {}", document.getTitle(), createdBy);

        // 验证文档信息
        validateDocument(document);

        // 验证分类是否存在
        if (document.getCategory() == null || document.getCategory().getId() == null) {
            throw new IllegalArgumentException("文档必须指定分类");
        }

        KnowledgeCategory category = categoryRepository.findById(document.getCategory().getId())
                .orElseThrow(() -> new IllegalArgumentException("指定的分类不存在"));

        // 设置文档信息
        document.setCategory(category);
        document.setCreatedBy(createdBy);
        document.setUpdatedBy(createdBy);

        // 如果没有设置摘要，自动生成
        if (document.getSummary() == null || document.getSummary().trim().isEmpty()) {
            document.setSummary(generateSummary(document.getContent()));
        }

        KnowledgeDocument savedDocument = documentRepository.save(document);

        // 异步创建搜索索引
        searchService.createOrUpdateIndex(savedDocument);

        // 异步处理文档向量化（RAG功能）
        embeddingService.processDocument(savedDocument);

        logger.info("成功创建知识文档: {}, ID: {}", savedDocument.getTitle(), savedDocument.getId());
        return savedDocument;
    }

    /**
     * 更新知识文档
     * 
     * @param documentId 文档ID
     * @param updatedDocument 更新的文档信息
     * @param updatedBy 更新者
     * @return 更新后的文档对象
     * @throws IllegalArgumentException 当文档不存在或信息无效时抛出
     */
    public KnowledgeDocument updateDocument(Long documentId, KnowledgeDocument updatedDocument, String updatedBy) {
        logger.info("更新知识文档: {}, 更新者: {}", documentId, updatedBy);

        KnowledgeDocument existingDocument = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在: " + documentId));

        // 验证更新信息
        validateDocument(updatedDocument);

        // 验证分类
        if (updatedDocument.getCategory() != null && updatedDocument.getCategory().getId() != null) {
            KnowledgeCategory category = categoryRepository.findById(updatedDocument.getCategory().getId())
                    .orElseThrow(() -> new IllegalArgumentException("指定的分类不存在"));
            existingDocument.setCategory(category);
        }

        // 更新基本信息
        existingDocument.setTitle(updatedDocument.getTitle());
        existingDocument.setContent(updatedDocument.getContent());
        existingDocument.setSummary(updatedDocument.getSummary());
        existingDocument.setTags(updatedDocument.getTags());
        existingDocument.setPriority(updatedDocument.getPriority());
        existingDocument.setUpdatedBy(updatedBy);

        // 如果内容发生变化，增加版本号
        if (!existingDocument.getContent().equals(updatedDocument.getContent())) {
            existingDocument.setVersion(existingDocument.getVersion() + 1);
        }

        // 如果没有设置摘要，自动生成
        if (existingDocument.getSummary() == null || existingDocument.getSummary().trim().isEmpty()) {
            existingDocument.setSummary(generateSummary(existingDocument.getContent()));
        }

        KnowledgeDocument savedDocument = documentRepository.save(existingDocument);

        // 异步更新搜索索引
        searchService.createOrUpdateIndex(savedDocument);

        // 如果内容发生变化，重新处理向量化
        if (!existingDocument.getContent().equals(updatedDocument.getContent())) {
            embeddingService.processDocument(savedDocument);
        }

        logger.info("成功更新知识文档: {}", savedDocument.getId());
        return savedDocument;
    }

    /**
     * 发布文档
     * 
     * @param documentId 文档ID
     * @param publishedBy 发布者
     * @return 发布后的文档对象
     */
    public KnowledgeDocument publishDocument(Long documentId, String publishedBy) {
        logger.info("发布知识文档: {}, 发布者: {}", documentId, publishedBy);

        KnowledgeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在: " + documentId));

        document.publish();
        document.setUpdatedBy(publishedBy);

        KnowledgeDocument savedDocument = documentRepository.save(document);

        // 发布后立即更新搜索索引
        searchService.createOrUpdateIndex(savedDocument);

        // 发布后确保向量化处理完成
        embeddingService.processDocument(savedDocument);

        logger.info("成功发布知识文档: {}", savedDocument.getId());
        return savedDocument;
    }

    /**
     * 归档文档
     * 
     * @param documentId 文档ID
     * @param archivedBy 归档者
     * @return 归档后的文档对象
     */
    public KnowledgeDocument archiveDocument(Long documentId, String archivedBy) {
        logger.info("归档知识文档: {}, 归档者: {}", documentId, archivedBy);

        KnowledgeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在: " + documentId));

        document.archive();
        document.setUpdatedBy(archivedBy);

        KnowledgeDocument savedDocument = documentRepository.save(document);

        // 归档后移除搜索索引
        searchService.removeIndex(savedDocument);

        logger.info("成功归档知识文档: {}", savedDocument.getId());
        return savedDocument;
    }

    /**
     * 删除文档
     * 
     * @param documentId 文档ID
     */
    public void deleteDocument(Long documentId) {
        logger.info("删除知识文档: {}", documentId);

        KnowledgeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在: " + documentId));

        // 删除搜索索引
        searchService.removeIndex(document);

        // 删除文档
        documentRepository.delete(document);

        logger.info("成功删除知识文档: {}", documentId);
    }

    /**
     * 根据ID查找文档
     * 
     * @param documentId 文档ID
     * @return 文档对象
     */
    @Transactional(readOnly = true)
    public Optional<KnowledgeDocument> findById(Long documentId) {
        return documentRepository.findById(documentId);
    }

    /**
     * 查看文档详情（增加访问次数）
     * 
     * @param documentId 文档ID
     * @return 文档对象
     */
    @Transactional
    public Optional<KnowledgeDocument> viewDocument(Long documentId) {
        Optional<KnowledgeDocument> documentOpt = documentRepository.findById(documentId);
        
        if (documentOpt.isPresent()) {
            // 异步增加访问次数
            documentRepository.incrementViewCount(documentId);
            logger.debug("增加文档访问次数: {}", documentId);
        }
        
        return documentOpt;
    }

    /**
     * 点赞文档
     * 
     * @param documentId 文档ID
     * @return 是否成功
     */
    @Transactional
    public boolean likeDocument(Long documentId) {
        int updated = documentRepository.incrementLikeCount(documentId);
        if (updated > 0) {
            logger.debug("增加文档点赞次数: {}", documentId);
            return true;
        }
        return false;
    }

    /**
     * 根据分类查找已发布的文档
     * 
     * @param categoryId 分类ID
     * @param pageable 分页参数
     * @return 文档分页结果
     */
    @Transactional(readOnly = true)
    public Page<KnowledgeDocument> findByCategory(Long categoryId, Pageable pageable) {
        KnowledgeCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在: " + categoryId));

        return documentRepository.findByCategoryAndStatusOrderByPriorityDescCreatedAtDesc(
                category, KnowledgeDocument.DocumentStatus.PUBLISHED, pageable);
    }

    /**
     * 根据状态查找文档
     * 
     * @param status 文档状态
     * @param pageable 分页参数
     * @return 文档分页结果
     */
    @Transactional(readOnly = true)
    public Page<KnowledgeDocument> findByStatus(KnowledgeDocument.DocumentStatus status, Pageable pageable) {
        return documentRepository.findByStatusOrderByUpdatedAtDesc(status, pageable);
    }

    /**
     * 搜索文档
     * 
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 匹配的文档分页结果
     */
    @Transactional(readOnly = true)
    public Page<KnowledgeDocument> searchDocuments(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return documentRepository.findByStatusOrderByUpdatedAtDesc(
                    KnowledgeDocument.DocumentStatus.PUBLISHED, pageable);
        }

        return documentRepository.searchByKeyword(
                keyword.trim(), KnowledgeDocument.DocumentStatus.PUBLISHED, pageable);
    }

    /**
     * 获取热门文档
     * 
     * @param pageable 分页参数
     * @return 热门文档分页结果
     */
    @Transactional(readOnly = true)
    public Page<KnowledgeDocument> getPopularDocuments(Pageable pageable) {
        return documentRepository.findByStatusOrderByViewCountDescCreatedAtDesc(
                KnowledgeDocument.DocumentStatus.PUBLISHED, pageable);
    }

    /**
     * 获取最新文档
     * 
     * @param pageable 分页参数
     * @return 最新文档分页结果
     */
    @Transactional(readOnly = true)
    public Page<KnowledgeDocument> getLatestDocuments(Pageable pageable) {
        return documentRepository.findByStatusOrderByPublishedAtDescCreatedAtDesc(
                KnowledgeDocument.DocumentStatus.PUBLISHED, pageable);
    }

    /**
     * 验证文档信息
     * 
     * @param document 文档对象
     * @throws IllegalArgumentException 当文档信息无效时抛出
     */
    private void validateDocument(KnowledgeDocument document) {
        if (document == null) {
            throw new IllegalArgumentException("文档对象不能为空");
        }

        if (document.getTitle() == null || document.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("文档标题不能为空");
        }

        if (document.getTitle().length() > 200) {
            throw new IllegalArgumentException("文档标题长度不能超过200字符");
        }

        if (document.getContent() == null || document.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("文档内容不能为空");
        }

        if (document.getSummary() != null && document.getSummary().length() > 1000) {
            throw new IllegalArgumentException("文档摘要长度不能超过1000字符");
        }
    }

    /**
     * 自动生成文档摘要
     * 
     * @param content 文档内容
     * @return 生成的摘要
     */
    private String generateSummary(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "";
        }

        // 简单的摘要生成：取前200个字符
        String cleanContent = content.replaceAll("<[^>]+>", "").trim(); // 移除HTML标签
        if (cleanContent.length() <= 200) {
            return cleanContent;
        }

        return cleanContent.substring(0, 200) + "...";
    }
}
