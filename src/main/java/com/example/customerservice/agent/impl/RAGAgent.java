package com.example.customerservice.agent.impl;

import com.example.customerservice.agent.core.*;
import com.example.customerservice.service.RAGService;
import com.example.customerservice.dto.RAGSearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
        
        return ragService.search(query, maxResults);
    }
    
    /**
     * 执行向量搜索
     */
    private List<RAGSearchResult> performVectorSearch(AgentTask task) {
        String query = task.getParameter("query", "");
        int topK = task.getParameter("topK", 10);
        
        return ragService.vectorSearch(query, topK);
    }
    
    /**
     * 执行混合搜索
     */
    private List<RAGSearchResult> performHybridSearch(AgentTask task) {
        String query = task.getParameter("query", "");
        int topK = task.getParameter("topK", 10);
        
        return ragService.hybridSearch(query, topK);
    }
    
    /**
     * 执行文档检索
     */
    private List<RAGSearchResult> performDocumentRetrieval(AgentTask task) {
        String query = task.getParameter("query", "");
        String category = task.getParameter("category", null);
        int maxResults = task.getParameter("maxResults", 10);
        
        if (category != null) {
            // 如果指定了分类，可以添加分类过滤逻辑
            return ragService.searchByCategory(query, category, maxResults);
        } else {
            return ragService.search(query, maxResults);
        }
    }
    
    /**
     * 执行上下文增强
     */
    private Map<String, Object> performContextEnhancement(AgentTask task) {
        String query = task.getParameter("query", "");
        String context = task.getParameter("context", "");
        int maxChunks = task.getParameter("maxChunks", 5);
        
        // 搜索相关文档
        List<RAGSearchResult> searchResults = ragService.search(query, maxChunks);
        
        // 构建增强上下文
        StringBuilder enhancedContext = new StringBuilder(context);
        if (!context.isEmpty()) {
            enhancedContext.append("\n\n");
        }
        
        enhancedContext.append("相关知识：\n");
        for (int i = 0; i < searchResults.size(); i++) {
            RAGSearchResult result = searchResults.get(i);
            enhancedContext.append(String.format("%d. %s\n", i + 1, result.getContent()));
        }
        
        return Map.of(
            "originalContext", context,
            "enhancedContext", enhancedContext.toString(),
            "retrievedChunks", searchResults,
            "chunkCount", searchResults.size()
        );
    }
    
    /**
     * 执行知识查询
     */
    private Map<String, Object> performKnowledgeQuery(AgentTask task) {
        String query = task.getParameter("query", "");
        boolean includeMetadata = task.getParameter("includeMetadata", true);
        int maxResults = task.getParameter("maxResults", 10);
        
        List<RAGSearchResult> results = ragService.search(query, maxResults);
        
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("query", query);
        response.put("results", results);
        response.put("resultCount", results.size());
        
        if (includeMetadata) {
            // 添加元数据信息
            Map<String, Object> metadata = new java.util.HashMap<>();
            metadata.put("searchTime", System.currentTimeMillis());
            metadata.put("hasResults", !results.isEmpty());
            
            if (!results.isEmpty()) {
                double avgScore = results.stream()
                        .mapToDouble(RAGSearchResult::getScore)
                        .average()
                        .orElse(0.0);
                metadata.put("averageScore", avgScore);
                metadata.put("maxScore", results.get(0).getScore());
                metadata.put("minScore", results.get(results.size() - 1).getScore());
            }
            
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
                Map<String, Object> ragStats = ragService.getStatistics();
                stats.put("ragService", ragStats);
            } catch (Exception e) {
                logger.warn("Failed to get RAG service statistics", e);
            }
        }
        
        return stats;
    }
}
