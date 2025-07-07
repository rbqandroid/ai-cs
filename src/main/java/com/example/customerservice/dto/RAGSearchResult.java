package com.example.customerservice.dto;

import com.example.customerservice.entity.KnowledgeDocument;

import java.util.List;

/**
 * RAG搜索结果DTO
 * 
 * <p>这个类封装了RAG（检索增强生成）搜索的结果，包含搜索到的知识文档
 * 和相关的元数据信息。用于在知识库搜索和AI生成之间传递数据。</p>
 * 
 * <h3>主要功能：</h3>
 * <ul>
 *   <li>封装搜索结果文档列表</li>
 *   <li>提供搜索相关性评分</li>
 *   <li>记录搜索执行时间</li>
 *   <li>支持不同类型的搜索结果</li>
 * </ul>
 * 
 * @author AI Assistant
 * @version 1.0.0
 * @since 2024-07-04
 */
public class RAGSearchResult {
    
    /**
     * 搜索到的知识文档列表
     */
    private List<KnowledgeDocument> documents;
    
    /**
     * 搜索查询字符串
     */
    private String query;
    
    /**
     * 搜索结果总数
     */
    private int totalResults;
    
    /**
     * 搜索执行时间（毫秒）
     */
    private long executionTime;
    
    /**
     * 搜索类型（如：关键词搜索、向量搜索、混合搜索等）
     */
    private String searchType;
    
    /**
     * 平均相关性评分
     */
    private double averageScore;
    
    /**
     * 是否有更多结果
     */
    private boolean hasMoreResults;
    
    /**
     * 默认构造函数
     */
    public RAGSearchResult() {
    }
    
    /**
     * 构造函数
     * 
     * @param documents 搜索到的文档列表
     * @param query 搜索查询
     * @param searchType 搜索类型
     */
    public RAGSearchResult(List<KnowledgeDocument> documents, String query, String searchType) {
        this.documents = documents;
        this.query = query;
        this.searchType = searchType;
        this.totalResults = documents != null ? documents.size() : 0;
    }
    
    // Getter和Setter方法
    
    /**
     * 获取搜索到的文档列表
     * 
     * @return 知识文档列表
     */
    public List<KnowledgeDocument> getDocuments() {
        return documents;
    }
    
    /**
     * 设置搜索到的文档列表
     * 
     * @param documents 知识文档列表
     */
    public void setDocuments(List<KnowledgeDocument> documents) {
        this.documents = documents;
        this.totalResults = documents != null ? documents.size() : 0;
    }
    
    /**
     * 获取搜索查询字符串
     * 
     * @return 查询字符串
     */
    public String getQuery() {
        return query;
    }
    
    /**
     * 设置搜索查询字符串
     * 
     * @param query 查询字符串
     */
    public void setQuery(String query) {
        this.query = query;
    }
    
    /**
     * 获取搜索结果总数
     * 
     * @return 结果总数
     */
    public int getTotalResults() {
        return totalResults;
    }
    
    /**
     * 设置搜索结果总数
     * 
     * @param totalResults 结果总数
     */
    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }
    
    /**
     * 获取搜索执行时间
     * 
     * @return 执行时间（毫秒）
     */
    public long getExecutionTime() {
        return executionTime;
    }
    
    /**
     * 设置搜索执行时间
     * 
     * @param executionTime 执行时间（毫秒）
     */
    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }
    
    /**
     * 获取搜索类型
     * 
     * @return 搜索类型
     */
    public String getSearchType() {
        return searchType;
    }
    
    /**
     * 设置搜索类型
     * 
     * @param searchType 搜索类型
     */
    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }
    
    /**
     * 获取平均相关性评分
     * 
     * @return 平均评分
     */
    public double getAverageScore() {
        return averageScore;
    }
    
    /**
     * 设置平均相关性评分
     * 
     * @param averageScore 平均评分
     */
    public void setAverageScore(double averageScore) {
        this.averageScore = averageScore;
    }
    
    /**
     * 检查是否有更多结果
     * 
     * @return 如果有更多结果返回true
     */
    public boolean isHasMoreResults() {
        return hasMoreResults;
    }
    
    /**
     * 设置是否有更多结果
     * 
     * @param hasMoreResults 是否有更多结果
     */
    public void setHasMoreResults(boolean hasMoreResults) {
        this.hasMoreResults = hasMoreResults;
    }
    
    /**
     * 检查搜索结果是否为空
     * 
     * @return 如果结果为空返回true
     */
    public boolean isEmpty() {
        return documents == null || documents.isEmpty();
    }
    
    /**
     * 获取第一个文档（如果存在）
     * 
     * @return 第一个知识文档，如果不存在返回null
     */
    public KnowledgeDocument getFirstDocument() {
        return (documents != null && !documents.isEmpty()) ? documents.get(0) : null;
    }
    
    @Override
    public String toString() {
        return "RAGSearchResult{" +
                "query='" + query + '\'' +
                ", totalResults=" + totalResults +
                ", executionTime=" + executionTime +
                ", searchType='" + searchType + '\'' +
                ", averageScore=" + averageScore +
                ", hasMoreResults=" + hasMoreResults +
                '}';
    }
}
