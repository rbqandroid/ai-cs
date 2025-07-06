package com.example.customerservice.controller;

import com.example.customerservice.service.RAGService;
import com.example.customerservice.service.EmbeddingService;
import com.example.customerservice.service.VectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * RAG功能管理控制器
 * 
 * 提供RAG功能的REST API接口，包括向量搜索、统计信息查询等功能。
 * 支持RAG功能的监控和管理。
 * 
 * @author AI Assistant
 * @version 1.0.0
 * @since 2025-07-06
 */
@RestController
@RequestMapping("/api/rag")
@CrossOrigin(origins = "*")
public class RAGController {

    private static final Logger logger = LoggerFactory.getLogger(RAGController.class);

    @Autowired
    private RAGService ragService;

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private VectorStoreService vectorStoreService;

    /**
     * 测试RAG检索功能
     * 
     * @param query 查询文本
     * @param maxResults 最大结果数
     * @return 检索结果
     */
    @PostMapping("/search")
    public ResponseEntity<?> searchSimilar(@RequestParam String query,
                                         @RequestParam(defaultValue = "5") int maxResults) {
        logger.debug("RAG搜索请求: {}, maxResults: {}", query, maxResults);

        try {
            RAGService.RAGContext context = ragService.retrieveAndGenerate(query);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                    "query", query,
                    "context", context.getContext(),
                    "chunkCount", context.getChunkCount(),
                    "documentCount", context.getDocumentCount(),
                    "averageSimilarity", context.getAverageSimilarity(),
                    "maxSimilarity", context.getMaxSimilarity(),
                    "searchResults", context.getSearchResults().stream()
                        .map(result -> Map.of(
                            "chunkId", result.getChunk().getId(),
                            "documentTitle", result.getChunk().getDocument().getTitle(),
                            "content", result.getChunk().getContent().length() > 200 ? 
                                result.getChunk().getContent().substring(0, 200) + "..." : 
                                result.getChunk().getContent(),
                            "similarity", result.getSimilarity()
                        ))
                        .toList()
                )
            ));
        } catch (Exception e) {
            logger.error("RAG搜索失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "搜索失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 向量相似度搜索
     * 
     * @param query 查询文本
     * @param topK 返回结果数
     * @return 相似度搜索结果
     */
    @PostMapping("/vector-search")
    public ResponseEntity<?> vectorSearch(@RequestParam String query,
                                        @RequestParam(defaultValue = "10") int topK) {
        logger.debug("向量搜索请求: {}, topK: {}", query, topK);

        try {
            List<VectorStoreService.SimilaritySearchResult> results = 
                vectorStoreService.searchSimilar(query, topK);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                    "query", query,
                    "resultCount", results.size(),
                    "results", results.stream()
                        .map(result -> Map.of(
                            "chunkId", result.getChunk().getId(),
                            "documentId", result.getChunk().getDocument().getId(),
                            "documentTitle", result.getChunk().getDocument().getTitle(),
                            "chunkIndex", result.getChunk().getChunkIndex(),
                            "content", result.getChunk().getContent(),
                            "similarity", result.getSimilarity()
                        ))
                        .toList()
                )
            ));
        } catch (Exception e) {
            logger.error("向量搜索失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "向量搜索失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 混合搜索（向量+关键词）
     * 
     * @param query 查询文本
     * @param topK 返回结果数
     * @return 混合搜索结果
     */
    @PostMapping("/hybrid-search")
    public ResponseEntity<?> hybridSearch(@RequestParam String query,
                                        @RequestParam(defaultValue = "10") int topK) {
        logger.debug("混合搜索请求: {}, topK: {}", query, topK);

        try {
            List<VectorStoreService.SimilaritySearchResult> results = 
                vectorStoreService.hybridSearch(query, topK);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                    "query", query,
                    "resultCount", results.size(),
                    "results", results.stream()
                        .map(result -> Map.of(
                            "chunkId", result.getChunk().getId(),
                            "documentId", result.getChunk().getDocument().getId(),
                            "documentTitle", result.getChunk().getDocument().getTitle(),
                            "chunkIndex", result.getChunk().getChunkIndex(),
                            "content", result.getChunk().getContent().length() > 300 ? 
                                result.getChunk().getContent().substring(0, 300) + "..." : 
                                result.getChunk().getContent(),
                            "similarity", result.getSimilarity()
                        ))
                        .toList()
                )
            ));
        } catch (Exception e) {
            logger.error("混合搜索失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "混合搜索失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取RAG统计信息
     * 
     * @return RAG统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getStatistics() {
        logger.debug("获取RAG统计信息");

        try {
            RAGService.RAGStatistics ragStats = ragService.getStatistics();
            EmbeddingService.EmbeddingStatistics embeddingStats = embeddingService.getStatistics();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                    "rag", Map.of(
                        "totalChunks", ragStats.getTotalChunks(),
                        "readyChunks", ragStats.getReadyChunks(),
                        "embeddingChunks", ragStats.getEmbeddingChunks(),
                        "readyRate", String.format("%.2f%%", ragStats.getReadyRate() * 100),
                        "embeddingRate", String.format("%.2f%%", ragStats.getEmbeddingRate() * 100),
                        "avgChunkSize", ragStats.getAvgChunkSize(),
                        "avgDimension", ragStats.getAvgDimension()
                    ),
                    "embedding", Map.of(
                        "totalChunks", embeddingStats.getTotalChunks(),
                        "readyChunks", embeddingStats.getReadyChunks(),
                        "pendingChunks", embeddingStats.getPendingChunks(),
                        "processingChunks", embeddingStats.getProcessingChunks(),
                        "errorChunks", embeddingStats.getErrorChunks(),
                        "embeddingChunks", embeddingStats.getEmbeddingChunks(),
                        "avgChunkSize", embeddingStats.getAvgChunkSize(),
                        "avgDimension", embeddingStats.getAvgDimension()
                    )
                )
            ));
        } catch (Exception e) {
            logger.error("获取RAG统计信息失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "获取统计信息失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 重新处理失败的分块
     * 
     * @return 处理结果
     */
    @PostMapping("/reprocess-failed")
    public ResponseEntity<?> reprocessFailedChunks() {
        logger.info("重新处理失败分块请求");

        try {
            embeddingService.reprocessFailedChunks();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "已启动失败分块重新处理任务"
            ));
        } catch (Exception e) {
            logger.error("重新处理失败分块时发生错误", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "重新处理失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 评估查询匹配度
     * 
     * @param query 查询文本
     * @return 匹配度评估结果
     */
    @PostMapping("/evaluate")
    public ResponseEntity<?> evaluateQuery(@RequestParam String query) {
        logger.debug("查询匹配度评估: {}", query);

        try {
            double matchScore = ragService.evaluateQueryMatch(query);
            
            String matchLevel;
            if (matchScore >= 0.8) {
                matchLevel = "高";
            } else if (matchScore >= 0.6) {
                matchLevel = "中";
            } else if (matchScore >= 0.4) {
                matchLevel = "低";
            } else {
                matchLevel = "很低";
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                    "query", query,
                    "matchScore", matchScore,
                    "matchLevel", matchLevel,
                    "recommendation", matchScore < 0.5 ? 
                        "建议完善知识库内容或使用更具体的查询词" : 
                        "查询与知识库匹配度良好"
                )
            ));
        } catch (Exception e) {
            logger.error("查询匹配度评估失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "评估失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 健康检查
     * 
     * @return 健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        try {
            EmbeddingService.EmbeddingStatistics stats = embeddingService.getStatistics();
            
            boolean isHealthy = stats.getErrorChunks() < stats.getTotalChunks() * 0.1; // 错误率低于10%
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "healthy", isHealthy,
                "message", isHealthy ? "RAG功能运行正常" : "RAG功能存在问题，请检查错误分块",
                "errorRate", stats.getTotalChunks() > 0 ? 
                    String.format("%.2f%%", (double) stats.getErrorChunks() / stats.getTotalChunks() * 100) : "0%"
            ));
        } catch (Exception e) {
            logger.error("RAG健康检查失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "healthy", false,
                "message", "健康检查失败: " + e.getMessage()
            ));
        }
    }
}
