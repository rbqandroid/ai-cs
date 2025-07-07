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
    void testRetrieveAndGenerate() {
        String query = "test query";
        
        // 模拟向量搜索结果
        List<VectorStoreService.SimilaritySearchResult> mockResults = Arrays.asList(
            new VectorStoreService.SimilaritySearchResult(testChunk1, 0.9),
            new VectorStoreService.SimilaritySearchResult(testChunk2, 0.8)
        );
        when(vectorStoreService.hybridSearch(eq(query), anyInt())).thenReturn(mockResults);
        
        // 执行检索和生成
        RAGService.RAGContext result = ragService.retrieveAndGenerate(query);
        
        // 验证结果
        assertNotNull(result);
        assertNotNull(result.getContext());
        assertEquals(2, result.getSearchResults().size());
        assertEquals(0.85, result.getAverageSimilarity(), 0.01); // (0.9 + 0.8) / 2
        
        // 验证方法调用
        verify(vectorStoreService).hybridSearch(eq(query), anyInt());
    }
    
    @Test
    void testRetrieveAndGenerateWithEmptyQuery() {
        String query = "";
        
        // 执行检索和生成
        RAGService.RAGContext result = ragService.retrieveAndGenerate(query);
        
        // 验证结果
        assertNotNull(result);
        assertEquals("", result.getContext());
        assertTrue(result.getSearchResults().isEmpty());
        assertEquals(0.0, result.getAverageSimilarity());
        
        // 不应该调用向量搜索服务
        verify(vectorStoreService, never()).hybridSearch(anyString(), anyInt());
    }
    
    @Test
    void testBuildEnhancedPrompt() {
        String query = "test query";
        String basePrompt = "You are a helpful assistant";
        
        // 模拟向量搜索结果
        List<VectorStoreService.SimilaritySearchResult> mockResults = Arrays.asList(
            new VectorStoreService.SimilaritySearchResult(testChunk1, 0.9)
        );
        when(vectorStoreService.hybridSearch(eq(query), anyInt())).thenReturn(mockResults);
        
        // 执行构建增强提示词
        String enhancedPrompt = ragService.buildEnhancedPrompt(query, basePrompt);
        
        // 验证结果
        assertNotNull(enhancedPrompt);
        assertTrue(enhancedPrompt.contains(basePrompt));
        assertTrue(enhancedPrompt.contains("相关知识库内容"));
        
        // 验证方法调用
        verify(vectorStoreService).hybridSearch(eq(query), anyInt());
    }
    
    @Test
    void testEvaluateQueryMatch() {
        String query = "test query";
        
        // 模拟向量搜索结果
        List<VectorStoreService.SimilaritySearchResult> mockResults = Arrays.asList(
            new VectorStoreService.SimilaritySearchResult(testChunk1, 0.9),
            new VectorStoreService.SimilaritySearchResult(testChunk2, 0.8)
        );
        when(vectorStoreService.hybridSearch(eq(query), anyInt())).thenReturn(mockResults);
        
        // 执行查询匹配度评估
        double matchScore = ragService.evaluateQueryMatch(query);
        
        // 验证结果
        assertEquals(0.85, matchScore, 0.01);
        
        // 验证方法调用
        verify(vectorStoreService).hybridSearch(eq(query), anyInt());
    }
    
    @Test
    void testGetDocumentChunks() {
        Long documentId = 1L;
        int maxChunks = 5;
        
        // 执行获取文档块
        List<DocumentChunk> chunks = ragService.getDocumentChunks(documentId, maxChunks);
        
        // 验证结果 - 当前实现返回空列表
        assertNotNull(chunks);
        assertTrue(chunks.isEmpty());
    }
    
    @Test
    void testGetStatistics() {
        // 模拟嵌入服务统计信息
        EmbeddingService.EmbeddingStatistics mockEmbeddingStats = 
            new EmbeddingService.EmbeddingStatistics(100L, 80L, 60L, 80L, 60L, 60L, 500.0, 768.0);
        when(embeddingService.getStatistics()).thenReturn(mockEmbeddingStats);
        
        // 执行获取统计信息
        RAGService.RAGStatistics stats = ragService.getStatistics();
        
        // 验证结果
        assertNotNull(stats);
        assertEquals(100L, stats.getTotalChunks());
        assertEquals(80L, stats.getReadyChunks());
        assertEquals(60L, stats.getEmbeddingChunks());
        assertEquals(500.0, stats.getAvgChunkSize());
        assertEquals(768.0, stats.getAvgDimension());
        
        // 验证方法调用
        verify(embeddingService).getStatistics();
    }
    
    @Test
    void testRetrieveAndGenerateWithException() {
        String query = "test query";
        
        // 模拟向量搜索服务抛出异常
        when(vectorStoreService.hybridSearch(eq(query), anyInt()))
            .thenThrow(new RuntimeException("Vector search failed"));
        
        // 执行检索和生成
        RAGService.RAGContext result = ragService.retrieveAndGenerate(query);
        
        // 验证结果 - 应该返回空上下文而不是抛出异常
        assertNotNull(result);
        assertEquals("", result.getContext());
        assertTrue(result.getSearchResults().isEmpty());
        assertEquals(0.0, result.getAverageSimilarity());
        
        // 验证方法调用
        verify(vectorStoreService).hybridSearch(eq(query), anyInt());
    }
    
    @Test
    void testBuildEnhancedPromptWithNoContext() {
        String query = "test query";
        String basePrompt = "You are a helpful assistant";
        
        // 模拟空的搜索结果
        when(vectorStoreService.hybridSearch(eq(query), anyInt())).thenReturn(Arrays.asList());
        
        // 执行构建增强提示词
        String enhancedPrompt = ragService.buildEnhancedPrompt(query, basePrompt);
        
        // 验证结果 - 应该返回原始提示词
        assertNotNull(enhancedPrompt);
        assertEquals(basePrompt, enhancedPrompt);
        
        // 验证方法调用
        verify(vectorStoreService).hybridSearch(eq(query), anyInt());
    }
    
    @Test
    void testRetrieveAndGenerateWithLowSimilarity() {
        String query = "test query";
        
        // 模拟低相似度的搜索结果
        List<VectorStoreService.SimilaritySearchResult> mockResults = Arrays.asList(
            new VectorStoreService.SimilaritySearchResult(testChunk1, 0.5), // 低于阈值0.7
            new VectorStoreService.SimilaritySearchResult(testChunk2, 0.6)  // 低于阈值0.7
        );
        when(vectorStoreService.hybridSearch(eq(query), anyInt())).thenReturn(mockResults);
        
        // 执行检索和生成
        RAGService.RAGContext result = ragService.retrieveAndGenerate(query);
        
        // 验证结果 - 应该过滤掉低相似度结果
        assertNotNull(result);
        assertEquals("", result.getContext());
        assertTrue(result.getSearchResults().isEmpty());
        assertEquals(0.0, result.getAverageSimilarity());
        
        // 验证方法调用
        verify(vectorStoreService).hybridSearch(eq(query), anyInt());
    }
}
