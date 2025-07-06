package com.example.customerservice.service;

import com.example.customerservice.entity.KnowledgeDocument;
import com.example.customerservice.entity.KnowledgeSearchIndex;
import com.example.customerservice.repository.KnowledgeDocumentRepository;
import com.example.customerservice.repository.KnowledgeSearchIndexRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 知识库搜索服务类
 * 
 * 提供知识库的搜索功能，包括关键词搜索、语义搜索和搜索索引的管理。
 * 支持异步索引构建和智能搜索结果排序。
 * 
 * @author AI Assistant
 * @version 1.0.0
 * @since 2025-07-06
 */
@Service
@Transactional
public class KnowledgeSearchService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeSearchService.class);

    @Autowired
    private KnowledgeDocumentRepository documentRepository;

    @Autowired
    private KnowledgeSearchIndexRepository searchIndexRepository;

    // 停用词列表（简化版）
    private static final Set<String> STOP_WORDS = Set.of(
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好", "自己", "这"
    );

    // 标点符号正则表达式
    private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("[\\p{Punct}\\s]+");

    /**
     * 创建或更新文档的搜索索引
     * 
     * @param document 知识文档
     */
    @Async
    public void createOrUpdateIndex(KnowledgeDocument document) {
        logger.info("开始为文档创建/更新搜索索引: {}", document.getId());

        try {
            // 查找现有索引
            Optional<KnowledgeSearchIndex> existingIndexOpt = 
                    searchIndexRepository.findByDocumentId(document.getId());

            KnowledgeSearchIndex searchIndex;
            if (existingIndexOpt.isPresent()) {
                searchIndex = existingIndexOpt.get();
                searchIndex.startBuilding();
            } else {
                searchIndex = new KnowledgeSearchIndex(document);
            }

            // 提取关键词
            String keywords = extractKeywords(document);
            searchIndex.setKeywords(keywords);

            // TODO: 集成Spring AI Embedding功能
            // 暂时不设置向量化表示，等Spring AI集成完成后添加
            // String embedding = generateEmbedding(document);
            // searchIndex.setEmbedding(embedding);

            // 标记索引为就绪状态
            searchIndex.markAsReady();

            searchIndexRepository.save(searchIndex);
            logger.info("成功创建/更新文档搜索索引: {}", document.getId());

        } catch (Exception e) {
            logger.error("创建/更新搜索索引失败: " + document.getId(), e);
            
            // 标记索引为错误状态
            Optional<KnowledgeSearchIndex> indexOpt = 
                    searchIndexRepository.findByDocumentId(document.getId());
            if (indexOpt.isPresent()) {
                KnowledgeSearchIndex index = indexOpt.get();
                index.markAsError(e.getMessage());
                searchIndexRepository.save(index);
            }
        }
    }

    /**
     * 移除文档的搜索索引
     * 
     * @param document 知识文档
     */
    @Async
    public void removeIndex(KnowledgeDocument document) {
        logger.info("移除文档搜索索引: {}", document.getId());

        try {
            Optional<KnowledgeSearchIndex> indexOpt = 
                    searchIndexRepository.findByDocumentId(document.getId());
            
            if (indexOpt.isPresent()) {
                searchIndexRepository.delete(indexOpt.get());
                logger.info("成功移除文档搜索索引: {}", document.getId());
            }
        } catch (Exception e) {
            logger.error("移除搜索索引失败: " + document.getId(), e);
        }
    }

    /**
     * 关键词搜索
     * 
     * @param keyword 搜索关键词
     * @param maxResults 最大结果数
     * @return 匹配的文档列表，按相关性排序
     */
    @Transactional(readOnly = true)
    public List<KnowledgeDocument> searchByKeyword(String keyword, int maxResults) {
        logger.debug("执行关键词搜索: {}", keyword);

        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // 清理和分词
        String cleanKeyword = cleanText(keyword);
        List<String> searchTerms = tokenize(cleanKeyword);

        if (searchTerms.isEmpty()) {
            return Collections.emptyList();
        }

        // 搜索匹配的索引
        List<KnowledgeSearchIndex> matchingIndexes = new ArrayList<>();
        for (String term : searchTerms) {
            List<KnowledgeSearchIndex> indexes = searchIndexRepository.findByKeywordsContaining(term);
            matchingIndexes.addAll(indexes);
        }

        // 去重并计算相关性分数
        Map<Long, DocumentScore> documentScores = new HashMap<>();
        for (KnowledgeSearchIndex index : matchingIndexes) {
            Long documentId = index.getDocument().getId();
            DocumentScore score = documentScores.computeIfAbsent(documentId, 
                    k -> new DocumentScore(index.getDocument()));
            
            // 计算相关性分数
            score.addScore(calculateRelevanceScore(index, searchTerms));
        }

        // 按分数排序并返回结果
        return documentScores.values().stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(maxResults)
                .map(DocumentScore::getDocument)
                .collect(Collectors.toList());
    }

    /**
     * 语义搜索（基于向量相似度）
     * 
     * @param query 查询文本
     * @param maxResults 最大结果数
     * @return 匹配的文档列表，按相似度排序
     */
    @Transactional(readOnly = true)
    public List<KnowledgeDocument> searchBySemantic(String query, int maxResults) {
        logger.debug("执行语义搜索: {}", query);

        // TODO: 实现基于向量相似度的语义搜索
        // 需要集成Spring AI的Embedding功能
        
        // 暂时返回关键词搜索结果
        return searchByKeyword(query, maxResults);
    }

    /**
     * 混合搜索（结合关键词和语义搜索）
     * 
     * @param query 查询文本
     * @param maxResults 最大结果数
     * @return 匹配的文档列表
     */
    @Transactional(readOnly = true)
    public List<KnowledgeDocument> hybridSearch(String query, int maxResults) {
        logger.debug("执行混合搜索: {}", query);

        // 获取关键词搜索结果
        List<KnowledgeDocument> keywordResults = searchByKeyword(query, maxResults);
        
        // 获取语义搜索结果
        List<KnowledgeDocument> semanticResults = searchBySemantic(query, maxResults);
        
        // 合并和去重结果
        Set<Long> seenIds = new HashSet<>();
        List<KnowledgeDocument> combinedResults = new ArrayList<>();
        
        // 优先添加关键词搜索结果
        for (KnowledgeDocument doc : keywordResults) {
            if (seenIds.add(doc.getId())) {
                combinedResults.add(doc);
            }
        }
        
        // 添加语义搜索结果
        for (KnowledgeDocument doc : semanticResults) {
            if (seenIds.add(doc.getId()) && combinedResults.size() < maxResults) {
                combinedResults.add(doc);
            }
        }
        
        return combinedResults;
    }

    /**
     * 查找相关文档
     * 
     * @param documentId 当前文档ID
     * @param maxResults 最大结果数
     * @return 相关文档列表
     */
    @Transactional(readOnly = true)
    public List<KnowledgeDocument> findRelatedDocuments(Long documentId, int maxResults) {
        logger.debug("查找相关文档: {}", documentId);

        Optional<KnowledgeDocument> documentOpt = documentRepository.findById(documentId);
        if (!documentOpt.isPresent()) {
            return Collections.emptyList();
        }

        KnowledgeDocument document = documentOpt.get();
        
        // 基于分类查找相关文档
        return documentRepository.findSimilarDocuments(
                documentId,
                document.getCategory().getId(),
                PageRequest.of(0, maxResults)
        ).getContent();
    }

    /**
     * 重建所有搜索索引
     */
    @Async
    public void rebuildAllIndexes() {
        logger.info("开始重建所有搜索索引");

        try {
            // 查找所有已发布的文档
            List<KnowledgeDocument> documents = documentRepository.findByStatus(
                    KnowledgeDocument.DocumentStatus.PUBLISHED);

            for (KnowledgeDocument document : documents) {
                createOrUpdateIndex(document);
            }

            logger.info("完成重建所有搜索索引，共处理 {} 个文档", documents.size());
        } catch (Exception e) {
            logger.error("重建搜索索引失败", e);
        }
    }

    /**
     * 从文档中提取关键词
     * 
     * @param document 知识文档
     * @return 关键词字符串
     */
    private String extractKeywords(KnowledgeDocument document) {
        StringBuilder keywords = new StringBuilder();

        // 添加标题关键词（权重更高）
        String titleKeywords = extractKeywordsFromText(document.getTitle());
        keywords.append(titleKeywords).append(" ");

        // 添加内容关键词
        String contentKeywords = extractKeywordsFromText(document.getContent());
        keywords.append(contentKeywords).append(" ");

        // 添加摘要关键词
        if (document.getSummary() != null) {
            String summaryKeywords = extractKeywordsFromText(document.getSummary());
            keywords.append(summaryKeywords).append(" ");
        }

        // 添加标签
        if (document.getTags() != null) {
            keywords.append(document.getTags()).append(" ");
        }

        return keywords.toString().trim();
    }

    /**
     * 从文本中提取关键词
     * 
     * @param text 文本内容
     * @return 关键词字符串
     */
    private String extractKeywordsFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        // 清理文本
        String cleanText = cleanText(text);
        
        // 分词
        List<String> tokens = tokenize(cleanText);
        
        // 过滤停用词和短词
        List<String> keywords = tokens.stream()
                .filter(token -> token.length() > 1)
                .filter(token -> !STOP_WORDS.contains(token))
                .distinct()
                .collect(Collectors.toList());

        return String.join(" ", keywords);
    }

    /**
     * 清理文本
     * 
     * @param text 原始文本
     * @return 清理后的文本
     */
    private String cleanText(String text) {
        if (text == null) {
            return "";
        }

        // 移除HTML标签
        String cleanText = text.replaceAll("<[^>]+>", " ");
        
        // 转换为小写
        cleanText = cleanText.toLowerCase();
        
        // 移除多余的空白字符
        cleanText = cleanText.replaceAll("\\s+", " ");
        
        return cleanText.trim();
    }

    /**
     * 文本分词
     * 
     * @param text 文本内容
     * @return 词汇列表
     */
    private List<String> tokenize(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // 简单的分词实现：按标点符号和空白字符分割
        String[] tokens = PUNCTUATION_PATTERN.split(text);
        
        return Arrays.stream(tokens)
                .filter(token -> !token.trim().isEmpty())
                .map(String::trim)
                .collect(Collectors.toList());
    }

    /**
     * 计算相关性分数
     * 
     * @param index 搜索索引
     * @param searchTerms 搜索词列表
     * @return 相关性分数
     */
    private double calculateRelevanceScore(KnowledgeSearchIndex index, List<String> searchTerms) {
        double score = 0.0;
        String keywords = index.getKeywords().toLowerCase();
        
        for (String term : searchTerms) {
            // 计算词频
            int frequency = countOccurrences(keywords, term.toLowerCase());
            score += frequency;
        }
        
        // 考虑文档的访问次数和点赞数
        KnowledgeDocument document = index.getDocument();
        score += document.getViewCount() * 0.01; // 访问次数权重
        score += document.getLikeCount() * 0.1;  // 点赞数权重
        score += document.getPriority() * 10;    // 优先级权重
        
        return score;
    }

    /**
     * 计算字符串中子字符串的出现次数
     * 
     * @param text 文本
     * @param substring 子字符串
     * @return 出现次数
     */
    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        
        return count;
    }

    /**
     * 文档分数类
     * 用于搜索结果排序
     */
    private static class DocumentScore {
        private final KnowledgeDocument document;
        private double score;

        public DocumentScore(KnowledgeDocument document) {
            this.document = document;
            this.score = 0.0;
        }

        public void addScore(double additionalScore) {
            this.score += additionalScore;
        }

        public KnowledgeDocument getDocument() {
            return document;
        }

        public double getScore() {
            return score;
        }
    }
}
