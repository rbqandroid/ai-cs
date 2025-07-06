package com.example.customerservice.service;

import com.example.customerservice.entity.DocumentChunk;
import com.example.customerservice.entity.KnowledgeDocument;
import com.example.customerservice.repository.DocumentChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * 向量化服务类
 * 
 * 提供文档内容的向量化功能，支持文档分块、批量处理和异步向量化。
 * 集成Spring AI的Embedding模型，为RAG功能提供向量表示支持。
 * 
 * @author AI Assistant
 * @version 1.0.0
 * @since 2025-07-06
 */
@Service
@Transactional
public class EmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);

    @Autowired
    private DocumentChunkRepository chunkRepository;

    @Autowired(required = false)
    private EmbeddingModel embeddingModel;

    @Value("${rag.chunk.size:1000}")
    private int defaultChunkSize;

    @Value("${rag.chunk.overlap:200}")
    private int defaultOverlapSize;

    @Value("${rag.embedding.batch-size:10}")
    private int batchSize;

    @Value("${rag.embedding.enabled:true}")
    private boolean embeddingEnabled;

    // 文本清理正则表达式
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern MULTIPLE_SPACES_PATTERN = Pattern.compile("\\s+");
    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[\\r\\n\\t]+");

    /**
     * 为知识文档创建分块并生成向量
     * 
     * @param document 知识文档
     * @return 异步任务结果
     */
    @Async
    public CompletableFuture<Void> processDocument(KnowledgeDocument document) {
        logger.info("开始处理文档向量化: {}", document.getId());

        try {
            // 1. 删除现有分块
            chunkRepository.deleteByDocument(document);

            // 2. 创建新分块
            List<DocumentChunk> chunks = createChunks(document);

            // 3. 保存分块
            chunkRepository.saveAll(chunks);

            // 4. 生成向量（如果启用）
            if (embeddingEnabled && embeddingModel != null) {
                generateEmbeddingsForChunks(chunks);
            } else {
                logger.warn("向量化功能未启用或EmbeddingModel未配置，跳过向量生成");
                // 标记为就绪状态（无向量）
                chunks.forEach(chunk -> chunk.markAsReady());
                chunkRepository.saveAll(chunks);
            }

            logger.info("文档向量化处理完成: {}, 共创建 {} 个分块", document.getId(), chunks.size());
            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            logger.error("文档向量化处理失败: " + document.getId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 为单个文本生成向量
     * 
     * @param text 文本内容
     * @return 向量数组，如果失败返回null
     */
    public float[] generateEmbedding(String text) {
        if (!embeddingEnabled || embeddingModel == null) {
            logger.debug("向量化功能未启用，返回null");
            return null;
        }

        if (text == null || text.trim().isEmpty()) {
            logger.warn("文本内容为空，无法生成向量");
            return null;
        }

        try {
            String cleanText = cleanText(text);
            EmbeddingRequest request = new EmbeddingRequest(List.of(cleanText), null);
            EmbeddingResponse response = embeddingModel.call(request);

            if (response.getResults() != null && !response.getResults().isEmpty()) {
                return response.getResults().get(0).getOutput();
            }

            logger.warn("向量化响应为空");
            return null;

        } catch (Exception e) {
            logger.error("生成向量失败: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * 批量生成向量
     * 
     * @param texts 文本列表
     * @return 向量列表
     */
    public List<float[]> generateEmbeddings(List<String> texts) {
        List<float[]> embeddings = new ArrayList<>();

        if (!embeddingEnabled || embeddingModel == null) {
            logger.debug("向量化功能未启用，返回空列表");
            return embeddings;
        }

        if (texts == null || texts.isEmpty()) {
            return embeddings;
        }

        try {
            // 清理文本
            List<String> cleanTexts = texts.stream()
                    .map(this::cleanText)
                    .filter(text -> !text.trim().isEmpty())
                    .toList();

            if (cleanTexts.isEmpty()) {
                return embeddings;
            }

            // 分批处理
            for (int i = 0; i < cleanTexts.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, cleanTexts.size());
                List<String> batch = cleanTexts.subList(i, endIndex);

                EmbeddingRequest request = new EmbeddingRequest(batch, null);
                EmbeddingResponse response = embeddingModel.call(request);

                if (response.getResults() != null) {
                    response.getResults().forEach(result -> 
                        embeddings.add(result.getOutput()));
                }
            }

            logger.debug("批量生成向量完成: {} 个文本", texts.size());
            return embeddings;

        } catch (Exception e) {
            logger.error("批量生成向量失败: " + e.getMessage(), e);
            return embeddings;
        }
    }

    /**
     * 创建文档分块
     * 
     * @param document 知识文档
     * @return 分块列表
     */
    private List<DocumentChunk> createChunks(KnowledgeDocument document) {
        List<DocumentChunk> chunks = new ArrayList<>();
        String content = document.getContent();

        if (content == null || content.trim().isEmpty()) {
            logger.warn("文档内容为空，无法创建分块: {}", document.getId());
            return chunks;
        }

        // 清理文本
        String cleanContent = cleanText(content);
        
        // 如果内容较短，创建单个分块
        if (cleanContent.length() <= defaultChunkSize) {
            DocumentChunk chunk = new DocumentChunk(document, 0, cleanContent);
            chunk.setStartPosition(0);
            chunk.setEndPosition(cleanContent.length());
            chunks.add(chunk);
            return chunks;
        }

        // 分割长文档
        int chunkIndex = 0;
        int startPos = 0;

        while (startPos < cleanContent.length()) {
            int endPos = Math.min(startPos + defaultChunkSize, cleanContent.length());
            
            // 尝试在句号、问号、感叹号处分割
            if (endPos < cleanContent.length()) {
                int lastSentenceEnd = findLastSentenceEnd(cleanContent, startPos, endPos);
                if (lastSentenceEnd > startPos + defaultChunkSize / 2) {
                    endPos = lastSentenceEnd + 1;
                }
            }

            String chunkContent = cleanContent.substring(startPos, endPos);
            DocumentChunk chunk = new DocumentChunk(document, chunkIndex, chunkContent);
            chunk.setStartPosition(startPos);
            chunk.setEndPosition(endPos);

            // 设置重叠长度
            if (chunkIndex > 0) {
                chunk.setOverlapLength(Math.min(defaultOverlapSize, chunkContent.length()));
            }

            chunks.add(chunk);
            chunkIndex++;

            // 计算下一个分块的起始位置（考虑重叠）
            startPos = Math.max(endPos - defaultOverlapSize, endPos);
        }

        logger.debug("为文档 {} 创建了 {} 个分块", document.getId(), chunks.size());
        return chunks;
    }

    /**
     * 为分块生成向量
     *
     * @param chunks 分块列表
     */
    private void generateEmbeddingsForChunks(List<DocumentChunk> chunks) {
        logger.debug("开始为 {} 个分块生成向量", chunks.size());

        // 标记为处理中
        chunks.forEach(chunk -> chunk.markAsProcessing());
        chunkRepository.saveAll(chunks);

        // 提取文本内容
        List<String> texts = chunks.stream()
                .map(DocumentChunk::getContent)
                .toList();

        // 生成向量
        List<float[]> embeddings = generateEmbeddings(texts);

        // 更新分块
        for (int i = 0; i < chunks.size() && i < embeddings.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            float[] embedding = embeddings.get(i);

            if (embedding != null && embedding.length > 0) {
                chunk.setEmbeddingArray(embedding);
                chunk.markAsReady();
            } else {
                chunk.markAsError("向量生成失败");
            }
        }

        // 保存更新
        chunkRepository.saveAll(chunks);
        logger.debug("分块向量生成完成");
    }

    /**
     * 查找最后一个句子结束位置
     * 
     * @param text 文本
     * @param start 开始位置
     * @param end 结束位置
     * @return 句子结束位置，如果没找到返回-1
     */
    private int findLastSentenceEnd(String text, int start, int end) {
        for (int i = end - 1; i >= start; i--) {
            char c = text.charAt(i);
            if (c == '。' || c == '！' || c == '？' || c == '.' || c == '!' || c == '?') {
                return i;
            }
        }
        return -1;
    }

    /**
     * 清理文本内容
     * 
     * @param text 原始文本
     * @return 清理后的文本
     */
    private String cleanText(String text) {
        if (text == null) {
            return "";
        }

        // 移除HTML标签
        String cleaned = HTML_TAG_PATTERN.matcher(text).replaceAll(" ");
        
        // 规范化换行符和制表符
        cleaned = SPECIAL_CHARS_PATTERN.matcher(cleaned).replaceAll(" ");
        
        // 合并多个空格
        cleaned = MULTIPLE_SPACES_PATTERN.matcher(cleaned).replaceAll(" ");
        
        return cleaned.trim();
    }

    /**
     * 重新处理失败的分块
     * 
     * @return 异步任务结果
     */
    @Async
    public CompletableFuture<Void> reprocessFailedChunks() {
        logger.info("开始重新处理失败的分块");

        try {
            List<DocumentChunk> failedChunks = chunkRepository.findByStatus(DocumentChunk.ChunkStatus.ERROR);
            
            if (failedChunks.isEmpty()) {
                logger.info("没有失败的分块需要重新处理");
                return CompletableFuture.completedFuture(null);
            }

            logger.info("找到 {} 个失败的分块，开始重新处理", failedChunks.size());

            if (embeddingEnabled && embeddingModel != null) {
                generateEmbeddingsForChunks(failedChunks);
            } else {
                // 如果向量化未启用，直接标记为就绪
                failedChunks.forEach(chunk -> chunk.markAsReady());
                chunkRepository.saveAll(failedChunks);
            }

            logger.info("失败分块重新处理完成");
            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            logger.error("重新处理失败分块时发生错误", e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 获取向量化统计信息
     * 
     * @return 统计信息
     */
    @Transactional(readOnly = true)
    public EmbeddingStatistics getStatistics() {
        Object[] stats = chunkRepository.getChunkStatistics();
        
        if (stats != null && stats.length >= 8) {
            return new EmbeddingStatistics(
                ((Number) stats[0]).longValue(),  // totalCount
                ((Number) stats[1]).longValue(),  // readyCount
                ((Number) stats[2]).longValue(),  // pendingCount
                ((Number) stats[3]).longValue(),  // processingCount
                ((Number) stats[4]).longValue(),  // errorCount
                ((Number) stats[5]).longValue(),  // embeddingCount
                stats[6] != null ? ((Number) stats[6]).doubleValue() : 0.0,  // avgChunkSize
                stats[7] != null ? ((Number) stats[7]).doubleValue() : 0.0   // avgDimension
            );
        }

        return new EmbeddingStatistics(0, 0, 0, 0, 0, 0, 0.0, 0.0);
    }

    /**
     * 向量化统计信息类
     */
    public static class EmbeddingStatistics {
        private final long totalChunks;
        private final long readyChunks;
        private final long pendingChunks;
        private final long processingChunks;
        private final long errorChunks;
        private final long embeddingChunks;
        private final double avgChunkSize;
        private final double avgDimension;

        public EmbeddingStatistics(long totalChunks, long readyChunks, long pendingChunks, 
                                 long processingChunks, long errorChunks, long embeddingChunks,
                                 double avgChunkSize, double avgDimension) {
            this.totalChunks = totalChunks;
            this.readyChunks = readyChunks;
            this.pendingChunks = pendingChunks;
            this.processingChunks = processingChunks;
            this.errorChunks = errorChunks;
            this.embeddingChunks = embeddingChunks;
            this.avgChunkSize = avgChunkSize;
            this.avgDimension = avgDimension;
        }

        // Getter方法
        public long getTotalChunks() { return totalChunks; }
        public long getReadyChunks() { return readyChunks; }
        public long getPendingChunks() { return pendingChunks; }
        public long getProcessingChunks() { return processingChunks; }
        public long getErrorChunks() { return errorChunks; }
        public long getEmbeddingChunks() { return embeddingChunks; }
        public double getAvgChunkSize() { return avgChunkSize; }
        public double getAvgDimension() { return avgDimension; }
    }
}
