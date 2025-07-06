package com.example.customerservice.service;

import com.example.customerservice.dto.RAGSearchResult;
import com.example.customerservice.entity.DocumentChunk;
import com.example.customerservice.entity.KnowledgeDocument;
import com.example.customerservice.repository.DocumentChunkRepository;
import com.example.customerservice.repository.KnowledgeDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RAGService单元测试
 * 
 * @author AI Customer Service Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class RAGServiceTest {
    
    @Mock
    private DocumentChunkRepository documentChunkRepository;
    
    @Mock
    private KnowledgeDocumentRepository knowledgeDocumentRepository;
    
    @Mock
    private EmbeddingService embeddingService;
    
    @Mock
    private VectorStoreService vectorStoreService;
    
    @InjectMocks
    private RAGService ragService;
    
    private KnowledgeDocument testDocument;
    private DocumentChunk testChunk1;
    private DocumentChunk testChunk2;
    
    @BeforeEach
    void setUp() {
        // 创建测试数据
        testDocument = new KnowledgeDocument();
        testDocument.setId(1L);
        testDocument.setTitle("Test Document");
        testDocument.setContent("This is a test document content");
        testDocument.setStatus(KnowledgeDocument.DocumentStatus.PUBLISHED);
        
        testChunk1 = new DocumentChunk();
        testChunk1.setId(1L);
        testChunk1.setDocument(testDocument);
        testChunk1.setContent("This is the first chunk");
        testChunk1.setChunkIndex(0);
        testChunk1.setStatus(DocumentChunk.ChunkStatus.READY);
        
        testChunk2 = new DocumentChunk();
        testChunk2.setId(2L);
        testChunk2.setDocument(testDocument);
        testChunk2.setContent("This is the second chunk");
        testChunk2.setChunkIndex(1);
        testChunk2.setStatus(DocumentChunk.ChunkStatus.READY);
    }
    
    @Test
    void testSearch() {
        String query = "test query";
        int maxResults = 5;
        
        List<DocumentChunk> mockChunks = Arrays.asList(testChunk1, testChunk2);
        when(documentChunkRepository.findByContentContainingIgnoreCaseAndStatus(
                eq(query), eq(DocumentChunk.ChunkStatus.READY), any(Pageable.class)))
                .thenReturn(mockChunks);
        
        // 执行搜索
        List<RAGSearchResult> results = ragService.search(query, maxResults);
        
        // 验证结果
        assertNotNull(results);
        assertEquals(2, results.size());
        
        RAGSearchResult result1 = results.get(0);
        assertEquals(testChunk1.getContent(), result1.getContent());
        assertEquals(testDocument.getTitle(), result1.getDocumentTitle());
        assertTrue(result1.getScore() > 0);
        
        // 验证方法调用
        verify(documentChunkRepository).findByContentContainingIgnoreCaseAndStatus(
                eq(query), eq(DocumentChunk.ChunkStatus.READY), any(Pageable.class));
    }
    
    @Test
    void testSearchWithEmptyQuery() {
        String query = "";
        int maxResults = 5;
        
        List<RAGSearchResult> results = ragService.search(query, maxResults);
        
        // 空查询应该返回空结果
        assertNotNull(results);
        assertTrue(results.isEmpty());
        
        // 不应该调用repository
        verify(documentChunkRepository, never()).findByContentContainingIgnoreCaseAndStatus(
                anyString(), any(), any(Pageable.class));
    }
    
    @Test
    void testVectorSearch() {
        String query = "test query";
        int topK = 10;
        
        // 模拟向量化结果
        List<Double> queryVector = Arrays.asList(0.1, 0.2, 0.3);
        when(embeddingService.embed(query)).thenReturn(queryVector);
        
        // 模拟向量搜索结果
        List<DocumentChunk> mockChunks = Arrays.asList(testChunk1, testChunk2);
        when(vectorStoreService.findSimilarChunks(queryVector, topK)).thenReturn(mockChunks);
        
        // 执行向量搜索
        List<RAGSearchResult> results = ragService.vectorSearch(query, topK);
        
        // 验证结果
        assertNotNull(results);
        assertEquals(2, results.size());
        
        // 验证方法调用
        verify(embeddingService).embed(query);
        verify(vectorStoreService).findSimilarChunks(queryVector, topK);
    }
    
    @Test
    void testVectorSearchWithEmbeddingFailure() {
        String query = "test query";
        int topK = 10;
        
        // 模拟向量化失败
        when(embeddingService.embed(query)).thenThrow(new RuntimeException("Embedding failed"));
        
        // 执行向量搜索
        List<RAGSearchResult> results = ragService.vectorSearch(query, topK);
        
        // 应该返回空结果
        assertNotNull(results);
        assertTrue(results.isEmpty());
        
        // 不应该调用向量存储服务
        verify(vectorStoreService, never()).findSimilarChunks(any(), anyInt());
    }
    
    @Test
    void testHybridSearch() {
        String query = "test query";
        int topK = 10;
        
        // 模拟关键词搜索结果
        List<DocumentChunk> keywordChunks = Arrays.asList(testChunk1);
        when(documentChunkRepository.findByContentContainingIgnoreCaseAndStatus(
                eq(query), eq(DocumentChunk.ChunkStatus.READY), any(Pageable.class)))
                .thenReturn(keywordChunks);
        
        // 模拟向量搜索结果
        List<Double> queryVector = Arrays.asList(0.1, 0.2, 0.3);
        when(embeddingService.embed(query)).thenReturn(queryVector);
        
        List<DocumentChunk> vectorChunks = Arrays.asList(testChunk2);
        when(vectorStoreService.findSimilarChunks(queryVector, topK)).thenReturn(vectorChunks);
        
        // 执行混合搜索
        List<RAGSearchResult> results = ragService.hybridSearch(query, topK);
        
        // 验证结果（应该包含两个不同的chunk）
        assertNotNull(results);
        assertEquals(2, results.size());
        
        // 验证方法调用
        verify(documentChunkRepository).findByContentContainingIgnoreCaseAndStatus(
                eq(query), eq(DocumentChunk.ChunkStatus.READY), any(Pageable.class));
        verify(embeddingService).embed(query);
        verify(vectorStoreService).findSimilarChunks(queryVector, topK);
    }
    
    @Test
    void testSearchByCategory() {
        String query = "test query";
        String category = "test-category";
        int maxResults = 5;
        
        List<DocumentChunk> mockChunks = Arrays.asList(testChunk1, testChunk2);
        when(documentChunkRepository.findByContentContainingAndCategoryAndStatus(
                eq(query), eq(category), eq(DocumentChunk.ChunkStatus.READY), any(Pageable.class)))
                .thenReturn(mockChunks);
        
        // 执行分类搜索
        List<RAGSearchResult> results = ragService.searchByCategory(query, category, maxResults);
        
        // 验证结果
        assertNotNull(results);
        assertEquals(2, results.size());
        
        // 验证方法调用
        verify(documentChunkRepository).findByContentContainingAndCategoryAndStatus(
                eq(query), eq(category), eq(DocumentChunk.ChunkStatus.READY), any(Pageable.class));
    }
    
    @Test
    void testEvaluateQuery() {
        String query = "test query";
        
        // 模拟搜索结果
        List<DocumentChunk> mockChunks = Arrays.asList(testChunk1, testChunk2);
        when(documentChunkRepository.findByContentContainingIgnoreCaseAndStatus(
                eq(query), eq(DocumentChunk.ChunkStatus.READY), any(Pageable.class)))
                .thenReturn(mockChunks);
        
        // 执行查询评估
        Map<String, Object> evaluation = ragService.evaluateQuery(query);
        
        // 验证评估结果
        assertNotNull(evaluation);
        assertEquals(query, evaluation.get("query"));
        assertEquals(2, evaluation.get("totalResults"));
        assertTrue((Boolean) evaluation.get("hasResults"));
        assertTrue(evaluation.containsKey("averageScore"));
        assertTrue(evaluation.containsKey("maxScore"));
        assertTrue(evaluation.containsKey("searchTime"));
    }
    
    @Test
    void testEvaluateQueryWithNoResults() {
        String query = "no results query";
        
        // 模拟无搜索结果
        when(documentChunkRepository.findByContentContainingIgnoreCaseAndStatus(
                eq(query), eq(DocumentChunk.ChunkStatus.READY), any(Pageable.class)))
                .thenReturn(Arrays.asList());
        
        // 执行查询评估
        Map<String, Object> evaluation = ragService.evaluateQuery(query);
        
        // 验证评估结果
        assertNotNull(evaluation);
        assertEquals(query, evaluation.get("query"));
        assertEquals(0, evaluation.get("totalResults"));
        assertFalse((Boolean) evaluation.get("hasResults"));
        assertEquals(0.0, evaluation.get("averageScore"));
        assertEquals(0.0, evaluation.get("maxScore"));
    }
    
    @Test
    void testReprocessFailedChunks() {
        // 模拟失败的分块
        List<DocumentChunk> failedChunks = Arrays.asList(testChunk1, testChunk2);
        when(documentChunkRepository.findByStatus(DocumentChunk.ChunkStatus.FAILED))
                .thenReturn(failedChunks);
        
        // 模拟向量化成功
        List<Double> vector = Arrays.asList(0.1, 0.2, 0.3);
        when(embeddingService.embed(anyString())).thenReturn(vector);
        
        // 执行重新处理
        int reprocessedCount = ragService.reprocessFailedChunks();
        
        // 验证结果
        assertEquals(2, reprocessedCount);
        
        // 验证方法调用
        verify(documentChunkRepository).findByStatus(DocumentChunk.ChunkStatus.FAILED);
        verify(embeddingService, times(2)).embed(anyString());
        verify(vectorStoreService, times(2)).storeVector(anyLong(), any());
        verify(documentChunkRepository, times(2)).save(any(DocumentChunk.class));
        
        // 验证分块状态已更新
        assertEquals(DocumentChunk.ChunkStatus.READY, testChunk1.getStatus());
        assertEquals(DocumentChunk.ChunkStatus.READY, testChunk2.getStatus());
    }
    
    @Test
    void testReprocessFailedChunksWithEmbeddingFailure() {
        // 模拟失败的分块
        List<DocumentChunk> failedChunks = Arrays.asList(testChunk1);
        when(documentChunkRepository.findByStatus(DocumentChunk.ChunkStatus.FAILED))
                .thenReturn(failedChunks);
        
        // 模拟向量化失败
        when(embeddingService.embed(anyString())).thenThrow(new RuntimeException("Embedding failed"));
        
        // 执行重新处理
        int reprocessedCount = ragService.reprocessFailedChunks();
        
        // 应该返回0，因为处理失败
        assertEquals(0, reprocessedCount);
        
        // 分块状态应该保持为FAILED
        assertEquals(DocumentChunk.ChunkStatus.FAILED, testChunk1.getStatus());
    }
    
    @Test
    void testGetStatistics() {
        // 模拟统计数据
        when(documentChunkRepository.count()).thenReturn(100L);
        when(documentChunkRepository.countByStatus(DocumentChunk.ChunkStatus.READY)).thenReturn(80L);
        when(documentChunkRepository.countByStatus(DocumentChunk.ChunkStatus.PROCESSING)).thenReturn(10L);
        when(documentChunkRepository.countByStatus(DocumentChunk.ChunkStatus.FAILED)).thenReturn(10L);
        when(knowledgeDocumentRepository.count()).thenReturn(20L);
        
        // 获取统计信息
        Map<String, Object> statistics = ragService.getStatistics();
        
        // 验证统计信息
        assertNotNull(statistics);
        assertEquals(100L, statistics.get("totalChunks"));
        assertEquals(80L, statistics.get("readyChunks"));
        assertEquals(10L, statistics.get("processingChunks"));
        assertEquals(10L, statistics.get("failedChunks"));
        assertEquals(20L, statistics.get("totalDocuments"));
        assertTrue(statistics.containsKey("lastUpdated"));
    }
    
    @Test
    void testIsHealthy() {
        // 模拟健康状态
        when(documentChunkRepository.count()).thenReturn(100L);
        when(embeddingService.isHealthy()).thenReturn(true);
        when(vectorStoreService.isHealthy()).thenReturn(true);
        
        // 检查健康状态
        boolean isHealthy = ragService.isHealthy();
        
        // 验证健康状态
        assertTrue(isHealthy);
        
        // 验证方法调用
        verify(embeddingService).isHealthy();
        verify(vectorStoreService).isHealthy();
    }
    
    @Test
    void testIsHealthyWithUnhealthyDependencies() {
        // 模拟不健康的依赖
        when(embeddingService.isHealthy()).thenReturn(false);
        when(vectorStoreService.isHealthy()).thenReturn(true);
        
        // 检查健康状态
        boolean isHealthy = ragService.isHealthy();
        
        // 应该返回false
        assertFalse(isHealthy);
    }
}
