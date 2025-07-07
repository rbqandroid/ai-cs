package com.example.customerservice.agent.impl;

import com.example.customerservice.agent.core.*;
import com.example.customerservice.service.RAGService;
import com.example.customerservice.dto.RAGSearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RAG Agent实现
 * 处理检索增强生成相关任务
 * 
 * @author AI Customer Service Team
 * @since 1.0.0
 */
@Component
public class RAGAgent extends AbstractAgent {
    
    @Autowired
    private RAGService ragService;
    
    private AgentCapabilities capabilities;
    
    public RAGAgent() {
        super("rag-agent", "RAG Agent", "处理检索增强生成任务", AgentType.RAG);
        initializeCapabilities();
    }
    
    private void initializeCapabilities() {
        this.capabilities = AgentCapabilities.builder()
                .supportedTaskTypes(Set.of(
                    "rag_search",
                    "vector_search",
                    "hybrid_search",
                    "document_retrieval",
                    "context_enhancement",
                    "knowledge_query"
                ))
                .maxConcurrentTasks(20)
                .maxTaskDurationMs(60000) // 1分钟
                .supportsStreaming(false)
                .supportsCallback(true)
                .version("1.0.0")
                .description("RAG检索增强生成Agent，提供智能文档检索和上下文增强")
                .build();
    }
    
    @Override
    protected AgentResult doExecute(AgentTask task) {
        try {
            String taskType = task.getType();
            String query = task.getParameter("query", "");
            
            if (query.isEmpty()) {
                return AgentResult.failure(task.getId(), getId(), "Query parameter is required");
            }
            
            logger.info("Processing RAG task: {} with query: {}", taskType, query);
            
            Object result = switch (taskType) {
                case "rag_search" -> performRAGSearch(task);
                case "vector_search" -> performVectorSearch(task);
                case "hybrid_search" -> performHybridSearch(task);
                case "document_retrieval" -> performDocumentRetrieval(task);
                case "context_enhancement" -> performContextEnhancement(task);
                case "knowledge_query" -> performKnowledgeQuery(task);
                default -> performRAGSearch(task); // 默认使用RAG搜索
            };
            
            Map<String, Object> resultData = Map.of(
                "result", result,
                "query", query,
                "taskType", taskType,
                "timestamp", System.currentTimeMillis()
            );
            
            return AgentResult.success(task.getId(), getId(), resultData, "RAG task completed successfully");
            
        } catch (Exception e) {
            logger.error("Error processing RAG task: {}", task.getId(), e);
            return AgentResult.failure(task.getId(), getId(), "Failed to process RAG task: " + e.getMessage(), e);
        }
    }
    
    /**
     * 执行RAG搜索
     */
    private List<RAGSearchResult> performRAGSearch(AgentTask task) {
        String query = task.getParameter("query", "");
        int maxResults = task.getParameter("maxResults", 5);

        logger.debug("执行RAG搜索: query={}, maxResults={}", query, maxResults);
        // 注意：当前版本的RAGService.retrieveAndGenerate()方法内部已经处理了结果数量限制
        // 未来版本将支持通过maxResults参数动态控制搜索结果数量
        return convertToRAGSearchResults(ragService.retrieveAndGenerate(query), query, "RAG搜索");
    }

    /**
     * 执行向量搜索
     */
    private List<RAGSearchResult> performVectorSearch(AgentTask task) {
        String query = task.getParameter("query", "");
        int topK = task.getParameter("topK", 10);

        logger.debug("执行向量搜索: query={}, topK={}", query, topK);
        // 注意：当前版本使用RAGService的默认配置进行向量搜索
        // 未来版本将支持通过topK参数动态控制返回结果数量
        return convertToRAGSearchResults(ragService.retrieveAndGenerate(query), query, "向量搜索");
    }

    /**
     * 执行混合搜索
     */
    private List<RAGSearchResult> performHybridSearch(AgentTask task) {
        String query = task.getParameter("query", "");
        int topK = task.getParameter("topK", 10);

        logger.debug("执行混合搜索: query={}, topK={}", query, topK);
        // 注意：当前版本使用RAGService的默认配置进行混合搜索
        // 未来版本将支持通过topK参数动态控制返回结果数量
        return convertToRAGSearchResults(ragService.retrieveAndGenerate(query), query, "混合搜索");
    }

    /**
     * 执行文档检索
     */
    private List<RAGSearchResult> performDocumentRetrieval(AgentTask task) {
        String query = task.getParameter("query", "");
        String category = task.getParameter("category", null);
        int maxResults = task.getParameter("maxResults", 10);

        logger.debug("执行文档检索: query={}, category={}, maxResults={}", query, category, maxResults);
        // 目前使用通用的检索方法，未来可以根据category进行过滤
        // 当前版本的RAGService.retrieveAndGenerate()方法内部已经处理了结果数量限制
        // 未来版本将支持按分类过滤和动态控制结果数量
        return convertToRAGSearchResults(ragService.retrieveAndGenerate(query), query, "文档检索");
    }

    /**
     * 将RAGContext转换为RAGSearchResult列表
     */
    private List<RAGSearchResult> convertToRAGSearchResults(com.example.customerservice.service.RAGService.RAGContext ragContext, String query, String searchType) {
        List<RAGSearchResult> results = new ArrayList<>();

        if (ragContext != null && !ragContext.getContext().isEmpty()) {
            // 创建一个包含检索结果的RAGSearchResult
            RAGSearchResult result = new RAGSearchResult();
            result.setQuery(query);
            result.setSearchType(searchType);
            result.setAverageScore(ragContext.getAverageSimilarity());
            result.setTotalResults(ragContext.getSearchResults().size());

            // 将相似度搜索结果转换为知识文档
            List<com.example.customerservice.entity.KnowledgeDocument> documents = new ArrayList<>();
            for (var searchResult : ragContext.getSearchResults()) {
                // 根据实际的SimilaritySearchResult结构来转换
                // SimilaritySearchResult有getChunk()方法，然后调用getContent()
                com.example.customerservice.entity.KnowledgeDocument doc = new com.example.customerservice.entity.KnowledgeDocument();
                doc.setContent(searchResult.getChunk().getContent());
                doc.setTitle("检索结果 - " + searchResult.getChunk().getDocument().getTitle());
                doc.setCategory(searchResult.getChunk().getDocument().getCategory());
                documents.add(doc);
            }
            result.setDocuments(documents);

            results.add(result);
        }

        return results;
    }
    
    /**
     * 执行上下文增强
     */
    private Map<String, Object> performContextEnhancement(AgentTask task) {
        String query = task.getParameter("query", "");
        String context = task.getParameter("context", "");
        int maxChunks = task.getParameter("maxChunks", 5);

        logger.debug("执行上下文增强: query={}, contextLength={}, maxChunks={}",
                    query, context != null ? context.length() : 0, maxChunks);

        // 使用RAG服务检索相关内容
        // 注意：当前版本的RAGService.retrieveAndGenerate()方法内部已经处理了分块数量限制
        // 未来版本将支持通过maxChunks参数动态控制检索数量
        com.example.customerservice.service.RAGService.RAGContext ragContext = ragService.retrieveAndGenerate(query);

        // 构建增强上下文
        StringBuilder enhancedContext = new StringBuilder(context != null ? context : "");
        if (context != null && !context.isEmpty()) {
            enhancedContext.append("\n\n");
        }

        if (!ragContext.getContext().isEmpty()) {
            enhancedContext.append("相关知识：\n");
            enhancedContext.append(ragContext.getContext());
        }

        return Map.of(
            "originalContext", context,
            "enhancedContext", enhancedContext.toString(),
            "retrievedChunks", ragContext.getSearchResults(),
            "chunkCount", ragContext.getSearchResults().size(),
            "averageSimilarity", ragContext.getAverageSimilarity()
        );
    }
    
    /**
     * 执行知识查询
     */
    private Map<String, Object> performKnowledgeQuery(AgentTask task) {
        String query = task.getParameter("query", "");
        boolean includeMetadata = task.getParameter("includeMetadata", true);
        int maxResults = task.getParameter("maxResults", 10);

        logger.debug("执行知识查询: query={}, includeMetadata={}, maxResults={}",
                    query, includeMetadata, maxResults);

        // 使用RAG服务进行检索
        // 注意：当前版本的RAGService.retrieveAndGenerate()方法内部已经处理了结果数量限制
        // 未来版本将支持通过maxResults参数动态控制返回结果数量
        com.example.customerservice.service.RAGService.RAGContext ragContext = ragService.retrieveAndGenerate(query);
        List<RAGSearchResult> results = convertToRAGSearchResults(ragContext, query, "知识查询");

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("query", query);
        response.put("results", results);
        response.put("resultCount", results.size());
        response.put("context", ragContext.getContext());

        if (includeMetadata) {
            // 添加元数据信息
            Map<String, Object> metadata = new java.util.HashMap<>();
            metadata.put("searchTime", System.currentTimeMillis());
            metadata.put("hasResults", !results.isEmpty());
            metadata.put("averageSimilarity", ragContext.getAverageSimilarity());
            metadata.put("searchResultCount", ragContext.getSearchResults().size());

            response.put("metadata", metadata);
        }

        return response;
    }
    
    @Override
    public AgentCapabilities getCapabilities() {
        return capabilities;
    }
    
    @Override
    protected void doInitialize(Map<String, Object> config) {
        super.doInitialize(config);
        
        if (config != null) {
            // 从配置中读取RAG特定参数
            int maxConcurrentTasks = (Integer) config.getOrDefault("maxConcurrentTasks", 20);
            long maxTaskDuration = (Long) config.getOrDefault("maxTaskDurationMs", 60000L);
            
            // 更新能力配置
            this.capabilities = AgentCapabilities.builder()
                    .supportedTaskTypes(capabilities.getSupportedTaskTypes())
                    .maxConcurrentTasks(maxConcurrentTasks)
                    .maxTaskDurationMs(maxTaskDuration)
                    .supportsStreaming(capabilities.isSupportsStreaming())
                    .supportsCallback(capabilities.isSupportsCallback())
                    .version(capabilities.getVersion())
                    .description(capabilities.getDescription())
                    .build();
        }
        
        logger.info("RAG Agent initialized with capabilities: {}", capabilities);
    }
    
    @Override
    public boolean isHealthy() {
        return super.isHealthy() && ragService != null;
    }
    
    /**
     * 获取RAG统计信息
     */
    public Map<String, Object> getRAGStatistics() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.putAll(getStatistics().getPerformanceSummary().isEmpty() ? 
                    Map.of() : Map.of("performance", getStatistics().getPerformanceSummary()));
        
        // 添加RAG特定统计
        if (ragService != null) {
            try {
                com.example.customerservice.service.RAGService.RAGStatistics ragStats = ragService.getStatistics();
                Map<String, Object> ragStatsMap = new java.util.HashMap<>();
                ragStatsMap.put("totalChunks", ragStats.getTotalChunks());
                ragStatsMap.put("readyChunks", ragStats.getReadyChunks());
                ragStatsMap.put("embeddingChunks", ragStats.getEmbeddingChunks());
                ragStatsMap.put("avgChunkSize", ragStats.getAvgChunkSize());
                ragStatsMap.put("avgDimension", ragStats.getAvgDimension());
                ragStatsMap.put("readyRate", ragStats.getReadyRate());
                ragStatsMap.put("embeddingRate", ragStats.getEmbeddingRate());
                stats.put("ragService", ragStatsMap);
            } catch (Exception e) {
                logger.warn("Failed to get RAG service statistics", e);
            }
        }
        
        return stats;
    }
}
