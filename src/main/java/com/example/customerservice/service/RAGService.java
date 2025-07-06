package com.example.customerservice.service;

import com.example.customerservice.entity.DocumentChunk;
import com.example.customerservice.entity.KnowledgeDocument;
import com.example.customerservice.service.VectorStoreService.SimilaritySearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RAG（Retrieval-Augmented Generation）核心服务类
 * 
 * 整合检索和生成功能，为AI对话提供基于知识库的上下文增强。
 * 实现智能文档检索、上下文构建和结果优化等核心RAG功能。
 * 
 * @author AI Assistant
 * @version 1.0.0
 * @since 2025-07-06
 */
@Service
@Transactional(readOnly = true)
public class RAGService {

    private static final Logger logger = LoggerFactory.getLogger(RAGService.class);

    @Autowired
    private VectorStoreService vectorStoreService;

    @Autowired
    private EmbeddingService embeddingService;

    @Value("${rag.retrieval.max-chunks:5}")
    private int maxRetrievalChunks;

    @Value("${rag.context.max-length:2000}")
    private int maxContextLength;

    @Value("${rag.retrieval.similarity-threshold:0.7}")
    private double similarityThreshold;

    @Value("${rag.context.include-metadata:true}")
    private boolean includeMetadata;

    @Value("${rag.context.deduplicate:true}")
    private boolean enableDeduplication;

    /**
     * 基于查询检索相关文档并构建上下文
     * 
     * @param query 用户查询
     * @return RAG上下文结果
     */
    public RAGContext retrieveAndGenerate(String query) {
        logger.debug("执行RAG检索: {}", query);

        if (query == null || query.trim().isEmpty()) {
            return new RAGContext("", new ArrayList<>(), 0.0);
        }

        try {
            // 1. 检索相关文档片段
            List<SimilaritySearchResult> searchResults = vectorStoreService.hybridSearch(
                query, maxRetrievalChunks);

            if (searchResults.isEmpty()) {
                logger.debug("未找到相关文档片段");
                return new RAGContext("", new ArrayList<>(), 0.0);
            }

            // 2. 过滤低相似度结果
            List<SimilaritySearchResult> filteredResults = searchResults.stream()
                .filter(result -> result.getSimilarity() >= similarityThreshold)
                .collect(Collectors.toList());

            if (filteredResults.isEmpty()) {
                logger.debug("所有结果相似度低于阈值 {}", similarityThreshold);
                return new RAGContext("", new ArrayList<>(), 0.0);
            }

            // 3. 构建上下文
            String context = buildContext(filteredResults, query);

            // 4. 计算平均相似度
            double avgSimilarity = filteredResults.stream()
                .mapToDouble(SimilaritySearchResult::getSimilarity)
                .average()
                .orElse(0.0);

            logger.debug("RAG检索完成，找到 {} 个相关片段，平均相似度: {:.3f}", 
                filteredResults.size(), avgSimilarity);

            return new RAGContext(context, filteredResults, avgSimilarity);

        } catch (Exception e) {
            logger.error("RAG检索失败: " + e.getMessage(), e);
            return new RAGContext("", new ArrayList<>(), 0.0);
        }
    }

    /**
     * 构建增强的系统提示词
     * 
     * @param query 用户查询
     * @param basePrompt 基础提示词
     * @return 增强后的提示词
     */
    public String buildEnhancedPrompt(String query, String basePrompt) {
        RAGContext ragContext = retrieveAndGenerate(query);
        
        if (ragContext.getContext().isEmpty()) {
            return basePrompt;
        }

        StringBuilder enhancedPrompt = new StringBuilder();
        enhancedPrompt.append(basePrompt);
        
        enhancedPrompt.append("\n\n=== 相关知识库内容 ===\n");
        enhancedPrompt.append(ragContext.getContext());
        
        enhancedPrompt.append("\n\n=== 回答指导 ===\n");
        enhancedPrompt.append("请基于以上知识库内容回答用户问题。如果知识库中没有直接相关的信息，请诚实说明，");
        enhancedPrompt.append("并提供你能给出的一般性建议。请确保回答准确、有用且友好。");
        
        if (ragContext.getAverageSimilarity() < 0.8) {
            enhancedPrompt.append("\n注意：检索到的内容与问题的相关性可能不是很高，请谨慎使用。");
        }

        return enhancedPrompt.toString();
    }

    /**
     * 构建上下文文本
     * 
     * @param searchResults 搜索结果列表
     * @param query 原始查询
     * @return 构建的上下文文本
     */
    private String buildContext(List<SimilaritySearchResult> searchResults, String query) {
        StringBuilder contextBuilder = new StringBuilder();
        Set<Long> processedDocuments = new HashSet<>();
        int currentLength = 0;

        for (int i = 0; i < searchResults.size(); i++) {
            SimilaritySearchResult result = searchResults.get(i);
            DocumentChunk chunk = result.getChunk();
            KnowledgeDocument document = chunk.getDocument();

            // 去重检查（如果启用）
            if (enableDeduplication && processedDocuments.contains(document.getId())) {
                continue;
            }

            // 构建片段内容
            StringBuilder chunkContent = new StringBuilder();
            
            if (includeMetadata) {
                chunkContent.append(String.format("【文档：%s】\n", document.getTitle()));
                if (document.getSummary() != null && !document.getSummary().trim().isEmpty()) {
                    chunkContent.append(String.format("摘要：%s\n", document.getSummary()));
                }
                chunkContent.append(String.format("相似度：%.2f\n", result.getSimilarity()));
                chunkContent.append("内容：");
            }
            
            chunkContent.append(chunk.getContent());
            
            // 检查长度限制
            String chunkText = chunkContent.toString();
            if (currentLength + chunkText.length() > maxContextLength) {
                // 如果添加这个片段会超过长度限制，尝试截断
                int remainingLength = maxContextLength - currentLength;
                if (remainingLength > 100) { // 至少保留100个字符
                    chunkText = chunkText.substring(0, remainingLength - 3) + "...";
                } else {
                    break; // 空间不足，停止添加
                }
            }

            if (contextBuilder.length() > 0) {
                contextBuilder.append("\n\n");
            }
            contextBuilder.append(chunkText);
            currentLength += chunkText.length();

            processedDocuments.add(document.getId());

            // 检查是否达到长度限制
            if (currentLength >= maxContextLength) {
                break;
            }
        }

        return contextBuilder.toString();
    }

    /**
     * 获取文档的相关片段
     * 
     * @param documentId 文档ID
     * @param maxChunks 最大片段数
     * @return 相关片段列表
     */
    public List<DocumentChunk> getDocumentChunks(Long documentId, int maxChunks) {
        // 这里可以实现获取特定文档的分块逻辑
        // 暂时返回空列表，具体实现可以根据需要添加
        return new ArrayList<>();
    }

    /**
     * 评估查询与知识库的匹配度
     * 
     * @param query 用户查询
     * @return 匹配度分数（0-1之间）
     */
    public double evaluateQueryMatch(String query) {
        RAGContext context = retrieveAndGenerate(query);
        return context.getAverageSimilarity();
    }

    /**
     * 获取RAG统计信息
     * 
     * @return RAG统计信息
     */
    public RAGStatistics getStatistics() {
        EmbeddingService.EmbeddingStatistics embeddingStats = embeddingService.getStatistics();
        
        return new RAGStatistics(
            embeddingStats.getTotalChunks(),
            embeddingStats.getReadyChunks(),
            embeddingStats.getEmbeddingChunks(),
            embeddingStats.getAvgChunkSize(),
            embeddingStats.getAvgDimension()
        );
    }

    /**
     * RAG上下文结果类
     */
    public static class RAGContext {
        private final String context;
        private final List<SimilaritySearchResult> searchResults;
        private final double averageSimilarity;

        public RAGContext(String context, List<SimilaritySearchResult> searchResults, double averageSimilarity) {
            this.context = context;
            this.searchResults = searchResults;
            this.averageSimilarity = averageSimilarity;
        }

        public String getContext() {
            return context;
        }

        public List<SimilaritySearchResult> getSearchResults() {
            return searchResults;
        }

        public double getAverageSimilarity() {
            return averageSimilarity;
        }

        public boolean hasContext() {
            return context != null && !context.trim().isEmpty();
        }

        public int getChunkCount() {
            return searchResults.size();
        }

        /**
         * 获取涉及的文档数量
         */
        public int getDocumentCount() {
            Set<Long> documentIds = new HashSet<>();
            for (SimilaritySearchResult result : searchResults) {
                documentIds.add(result.getChunk().getDocument().getId());
            }
            return documentIds.size();
        }

        /**
         * 获取最高相似度
         */
        public double getMaxSimilarity() {
            return searchResults.stream()
                .mapToDouble(SimilaritySearchResult::getSimilarity)
                .max()
                .orElse(0.0);
        }

        @Override
        public String toString() {
            return String.format("RAGContext{chunkCount=%d, documentCount=%d, avgSimilarity=%.3f, contextLength=%d}",
                getChunkCount(), getDocumentCount(), averageSimilarity, 
                context != null ? context.length() : 0);
        }
    }

    /**
     * RAG统计信息类
     */
    public static class RAGStatistics {
        private final long totalChunks;
        private final long readyChunks;
        private final long embeddingChunks;
        private final double avgChunkSize;
        private final double avgDimension;

        public RAGStatistics(long totalChunks, long readyChunks, long embeddingChunks, 
                           double avgChunkSize, double avgDimension) {
            this.totalChunks = totalChunks;
            this.readyChunks = readyChunks;
            this.embeddingChunks = embeddingChunks;
            this.avgChunkSize = avgChunkSize;
            this.avgDimension = avgDimension;
        }

        public long getTotalChunks() { return totalChunks; }
        public long getReadyChunks() { return readyChunks; }
        public long getEmbeddingChunks() { return embeddingChunks; }
        public double getAvgChunkSize() { return avgChunkSize; }
        public double getAvgDimension() { return avgDimension; }

        public double getReadyRate() {
            return totalChunks > 0 ? (double) readyChunks / totalChunks : 0.0;
        }

        public double getEmbeddingRate() {
            return readyChunks > 0 ? (double) embeddingChunks / readyChunks : 0.0;
        }

        @Override
        public String toString() {
            return String.format("RAGStatistics{total=%d, ready=%d(%.1f%%), embedding=%d(%.1f%%), avgSize=%.1f, avgDim=%.1f}",
                totalChunks, readyChunks, getReadyRate() * 100, 
                embeddingChunks, getEmbeddingRate() * 100, avgChunkSize, avgDimension);
        }
    }
}
