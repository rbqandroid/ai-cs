package com.example.customerservice.service;

import com.example.customerservice.entity.DocumentChunk;
import com.example.customerservice.repository.DocumentChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 向量存储服务类
 * 
 * 提供向量存储和相似度搜索功能，支持基于余弦相似度的文档片段检索。
 * 为RAG功能提供高效的向量搜索能力。
 * 
 * @author AI Assistant
 * @version 1.0.0
 * @since 2025-07-06
 */
@Service
@Transactional(readOnly = true)
public class VectorStoreService {

    private static final Logger logger = LoggerFactory.getLogger(VectorStoreService.class);

    @Autowired
    private DocumentChunkRepository chunkRepository;

    @Autowired
    private EmbeddingService embeddingService;

    @Value("${rag.search.similarity-threshold:0.7}")
    private double similarityThreshold;

    @Value("${rag.search.max-results:10}")
    private int maxSearchResults;

    @Value("${rag.search.enable-reranking:true}")
    private boolean enableReranking;

    /**
     * 基于向量相似度搜索文档片段
     * 
     * @param query 查询文本
     * @param topK 返回的最相似结果数量
     * @return 相似度搜索结果列表
     */
    public List<SimilaritySearchResult> searchSimilar(String query, int topK) {
        logger.debug("执行向量相似度搜索: {}, topK: {}", query, topK);

        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // 1. 生成查询向量
            float[] queryEmbedding = embeddingService.generateEmbedding(query);
            if (queryEmbedding == null) {
                logger.warn("无法为查询生成向量，回退到关键词搜索");
                return fallbackToKeywordSearch(query, topK);
            }

            // 2. 获取所有已就绪的分块
            List<DocumentChunk> readyChunks = chunkRepository.findReadyChunksWithEmbedding();
            if (readyChunks.isEmpty()) {
                logger.warn("没有可用的向量化分块");
                return Collections.emptyList();
            }

            // 3. 计算相似度
            List<SimilaritySearchResult> results = new ArrayList<>();
            for (DocumentChunk chunk : readyChunks) {
                float[] chunkEmbedding = chunk.getEmbeddingArray();
                if (chunkEmbedding != null) {
                    double similarity = calculateCosineSimilarity(queryEmbedding, chunkEmbedding);
                    
                    if (similarity >= similarityThreshold) {
                        results.add(new SimilaritySearchResult(chunk, similarity));
                    }
                }
            }

            // 4. 排序并限制结果数量
            results.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));
            
            int resultCount = Math.min(topK, results.size());
            List<SimilaritySearchResult> topResults = results.subList(0, resultCount);

            // 5. 重排序（如果启用）
            if (enableReranking && topResults.size() > 1) {
                topResults = rerank(query, topResults);
            }

            logger.debug("向量搜索完成，返回 {} 个结果", topResults.size());
            return topResults;

        } catch (Exception e) {
            logger.error("向量搜索失败: " + e.getMessage(), e);
            return fallbackToKeywordSearch(query, topK);
        }
    }

    /**
     * 混合搜索：结合向量搜索和关键词搜索
     * 
     * @param query 查询文本
     * @param topK 返回的结果数量
     * @return 混合搜索结果
     */
    public List<SimilaritySearchResult> hybridSearch(String query, int topK) {
        logger.debug("执行混合搜索: {}, topK: {}", query, topK);

        // 1. 向量搜索
        List<SimilaritySearchResult> vectorResults = searchSimilar(query, topK);
        
        // 2. 关键词搜索
        List<SimilaritySearchResult> keywordResults = fallbackToKeywordSearch(query, topK);
        
        // 3. 合并和去重
        Map<Long, SimilaritySearchResult> mergedResults = new HashMap<>();
        
        // 添加向量搜索结果（优先级更高）
        for (SimilaritySearchResult result : vectorResults) {
            mergedResults.put(result.getChunk().getId(), result);
        }
        
        // 添加关键词搜索结果（如果不存在）
        for (SimilaritySearchResult result : keywordResults) {
            Long chunkId = result.getChunk().getId();
            if (!mergedResults.containsKey(chunkId)) {
                // 降低关键词搜索的相似度分数
                SimilaritySearchResult adjustedResult = new SimilaritySearchResult(
                    result.getChunk(), 
                    result.getSimilarity() * 0.8
                );
                mergedResults.put(chunkId, adjustedResult);
            }
        }
        
        // 4. 排序并返回
        List<SimilaritySearchResult> finalResults = new ArrayList<>(mergedResults.values());
        finalResults.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));
        
        int resultCount = Math.min(topK, finalResults.size());
        return finalResults.subList(0, resultCount);
    }

    /**
     * 查找相似文档片段
     * 
     * @param chunkId 参考分块ID
     * @param topK 返回的相似结果数量
     * @return 相似分块列表
     */
    public List<SimilaritySearchResult> findSimilarChunks(Long chunkId, int topK) {
        logger.debug("查找相似分块: {}, topK: {}", chunkId, topK);

        Optional<DocumentChunk> chunkOpt = chunkRepository.findById(chunkId);
        if (!chunkOpt.isPresent()) {
            logger.warn("分块不存在: {}", chunkId);
            return Collections.emptyList();
        }

        DocumentChunk referenceChunk = chunkOpt.get();
        float[] referenceEmbedding = referenceChunk.getEmbeddingArray();
        
        if (referenceEmbedding == null) {
            logger.warn("参考分块没有向量表示: {}", chunkId);
            return Collections.emptyList();
        }

        List<DocumentChunk> allChunks = chunkRepository.findReadyChunksWithEmbedding();
        List<SimilaritySearchResult> results = new ArrayList<>();

        for (DocumentChunk chunk : allChunks) {
            // 跳过自己
            if (chunk.getId().equals(chunkId)) {
                continue;
            }

            float[] chunkEmbedding = chunk.getEmbeddingArray();
            if (chunkEmbedding != null) {
                double similarity = calculateCosineSimilarity(referenceEmbedding, chunkEmbedding);
                
                if (similarity >= similarityThreshold) {
                    results.add(new SimilaritySearchResult(chunk, similarity));
                }
            }
        }

        // 排序并返回前K个结果
        results.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));
        int resultCount = Math.min(topK, results.size());
        
        return results.subList(0, resultCount);
    }

    /**
     * 计算余弦相似度
     * 
     * @param vector1 向量1
     * @param vector2 向量2
     * @return 余弦相似度值（0-1之间）
     */
    private double calculateCosineSimilarity(float[] vector1, float[] vector2) {
        if (vector1.length != vector2.length) {
            throw new IllegalArgumentException("向量维度不匹配");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }

        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * 关键词搜索回退方案
     * 
     * @param query 查询文本
     * @param topK 返回结果数量
     * @return 搜索结果列表
     */
    private List<SimilaritySearchResult> fallbackToKeywordSearch(String query, int topK) {
        logger.debug("使用关键词搜索作为回退方案");

        List<DocumentChunk> allChunks = chunkRepository.findByStatus(DocumentChunk.ChunkStatus.READY);
        List<SimilaritySearchResult> results = new ArrayList<>();

        String lowerQuery = query.toLowerCase();
        String[] keywords = lowerQuery.split("\\s+");

        for (DocumentChunk chunk : allChunks) {
            String content = chunk.getContent().toLowerCase();
            double score = calculateKeywordScore(content, keywords);
            
            if (score > 0) {
                results.add(new SimilaritySearchResult(chunk, score));
            }
        }

        // 排序并返回前K个结果
        results.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));
        int resultCount = Math.min(topK, results.size());
        
        return results.subList(0, resultCount);
    }

    /**
     * 计算关键词匹配分数
     * 
     * @param content 内容文本
     * @param keywords 关键词数组
     * @return 匹配分数
     */
    private double calculateKeywordScore(String content, String[] keywords) {
        double score = 0.0;
        int totalKeywords = keywords.length;

        for (String keyword : keywords) {
            if (keyword.trim().isEmpty()) {
                continue;
            }

            // 精确匹配
            if (content.contains(keyword)) {
                score += 1.0;
            }
            // 部分匹配
            else if (content.contains(keyword.substring(0, Math.min(keyword.length(), 3)))) {
                score += 0.5;
            }
        }

        return totalKeywords > 0 ? score / totalKeywords : 0.0;
    }

    /**
     * 重排序搜索结果
     * 基于多个因素进行重新排序，如文档质量、新鲜度等
     * 
     * @param query 原始查询
     * @param results 初始搜索结果
     * @return 重排序后的结果
     */
    private List<SimilaritySearchResult> rerank(String query, List<SimilaritySearchResult> results) {
        logger.debug("对 {} 个结果进行重排序", results.size());

        for (SimilaritySearchResult result : results) {
            DocumentChunk chunk = result.getChunk();
            double originalScore = result.getSimilarity();
            
            // 基础分数
            double newScore = originalScore;
            
            // 文档质量因子（基于文档的访问次数和点赞数）
            if (chunk.getDocument() != null) {
                double qualityFactor = 1.0 + 
                    (chunk.getDocument().getViewCount() * 0.001) + 
                    (chunk.getDocument().getLikeCount() * 0.01);
                newScore *= Math.min(qualityFactor, 1.5); // 最多提升50%
            }
            
            // 内容长度因子（适中长度的分块得分更高）
            int contentLength = chunk.getChunkSize();
            double lengthFactor = 1.0;
            if (contentLength >= 200 && contentLength <= 800) {
                lengthFactor = 1.1; // 适中长度加分
            } else if (contentLength < 100) {
                lengthFactor = 0.9; // 太短减分
            }
            newScore *= lengthFactor;
            
            // 更新分数
            result.setSimilarity(newScore);
        }

        // 重新排序
        results.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));
        return results;
    }

    /**
     * 相似度搜索结果类
     */
    public static class SimilaritySearchResult {
        private final DocumentChunk chunk;
        private double similarity;

        public SimilaritySearchResult(DocumentChunk chunk, double similarity) {
            this.chunk = chunk;
            this.similarity = similarity;
        }

        public DocumentChunk getChunk() {
            return chunk;
        }

        public double getSimilarity() {
            return similarity;
        }

        public void setSimilarity(double similarity) {
            this.similarity = similarity;
        }

        /**
         * 获取分块的上下文信息
         * 包括前后相邻的分块内容
         * 
         * @param contextSize 上下文大小（前后各多少个分块）
         * @return 上下文文本
         */
        public String getContextualContent(int contextSize) {
            // 这里可以实现获取相邻分块的逻辑
            // 暂时返回当前分块内容
            return chunk.getContent();
        }

        @Override
        public String toString() {
            return String.format("SimilaritySearchResult{chunkId=%d, similarity=%.4f, content='%s...'}",
                chunk.getId(), similarity, 
                chunk.getContent().length() > 50 ? 
                    chunk.getContent().substring(0, 50) : chunk.getContent());
        }
    }
}
