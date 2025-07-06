package com.example.customerservice.controller;

import com.example.customerservice.entity.KnowledgeDocument;
import com.example.customerservice.service.KnowledgeDocumentService;
import com.example.customerservice.service.KnowledgeSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 知识文档管理控制器
 * 
 * 提供知识文档的REST API接口，包括文档的创建、查询、更新、删除、搜索等功能。
 * 支持文档状态管理、访问统计和搜索功能。
 * 
 * @author AI Assistant
 * @version 1.0.0
 * @since 2025-07-06
 */
@RestController
@RequestMapping("/api/knowledge/documents")
@CrossOrigin(origins = "*") // 允许跨域访问，生产环境应该限制具体域名
public class KnowledgeDocumentController {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeDocumentController.class);

    @Autowired
    private KnowledgeDocumentService documentService;

    @Autowired
    private KnowledgeSearchService searchService;

    /**
     * 创建新的知识文档
     * 
     * @param document 文档信息
     * @return 创建的文档对象
     */
    @PostMapping
    public ResponseEntity<?> createDocument(@Valid @RequestBody KnowledgeDocument document) {
        logger.info("创建知识文档请求: {}", document.getTitle());

        try {
            // 从请求头或认证信息中获取创建者，这里暂时使用固定值
            String createdBy = "admin"; // TODO: 从认证信息中获取

            KnowledgeDocument createdDocument = documentService.createDocument(document, createdBy);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "文档创建成功",
                "data", createdDocument
            ));
        } catch (IllegalArgumentException e) {
            logger.warn("创建文档失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("创建文档时发生错误", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "服务器内部错误"
            ));
        }
    }

    /**
     * 根据ID获取文档详情
     * 
     * @param documentId 文档ID
     * @return 文档详情
     */
    @GetMapping("/{documentId}")
    public ResponseEntity<?> getDocumentById(@PathVariable Long documentId) {
        logger.debug("获取文档详情: {}", documentId);

        try {
            Optional<KnowledgeDocument> documentOpt = documentService.viewDocument(documentId);
            
            if (documentOpt.isPresent()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", documentOpt.get()
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("获取文档详情时发生错误", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "服务器内部错误"
            ));
        }
    }

    /**
     * 更新文档信息
     * 
     * @param documentId 文档ID
     * @param updatedDocument 更新的文档信息
     * @return 更新后的文档对象
     */
    @PutMapping("/{documentId}")
    public ResponseEntity<?> updateDocument(@PathVariable Long documentId, 
                                          @Valid @RequestBody KnowledgeDocument updatedDocument) {
        logger.info("更新文档请求: {}", documentId);

        try {
            // 从请求头或认证信息中获取更新者，这里暂时使用固定值
            String updatedBy = "admin"; // TODO: 从认证信息中获取

            KnowledgeDocument document = documentService.updateDocument(documentId, updatedDocument, updatedBy);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "文档更新成功",
                "data", document
            ));
        } catch (IllegalArgumentException e) {
            logger.warn("更新文档失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("更新文档时发生错误", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "服务器内部错误"
            ));
        }
    }

    /**
     * 发布文档
     * 
     * @param documentId 文档ID
     * @return 发布结果
     */
    @PostMapping("/{documentId}/publish")
    public ResponseEntity<?> publishDocument(@PathVariable Long documentId) {
        logger.info("发布文档请求: {}", documentId);

        try {
            String publishedBy = "admin"; // TODO: 从认证信息中获取

            KnowledgeDocument document = documentService.publishDocument(documentId, publishedBy);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "文档发布成功",
                "data", document
            ));
        } catch (IllegalArgumentException e) {
            logger.warn("发布文档失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("发布文档时发生错误", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "服务器内部错误"
            ));
        }
    }

    /**
     * 归档文档
     * 
     * @param documentId 文档ID
     * @return 归档结果
     */
    @PostMapping("/{documentId}/archive")
    public ResponseEntity<?> archiveDocument(@PathVariable Long documentId) {
        logger.info("归档文档请求: {}", documentId);

        try {
            String archivedBy = "admin"; // TODO: 从认证信息中获取

            KnowledgeDocument document = documentService.archiveDocument(documentId, archivedBy);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "文档归档成功",
                "data", document
            ));
        } catch (IllegalArgumentException e) {
            logger.warn("归档文档失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("归档文档时发生错误", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "服务器内部错误"
            ));
        }
    }

    /**
     * 删除文档
     * 
     * @param documentId 文档ID
     * @return 删除结果
     */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long documentId) {
        logger.info("删除文档请求: {}", documentId);

        try {
            documentService.deleteDocument(documentId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "文档删除成功"
            ));
        } catch (IllegalArgumentException e) {
            logger.warn("删除文档失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("删除文档时发生错误", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "服务器内部错误"
            ));
        }
    }

    /**
     * 点赞文档
     * 
     * @param documentId 文档ID
     * @return 点赞结果
     */
    @PostMapping("/{documentId}/like")
    public ResponseEntity<?> likeDocument(@PathVariable Long documentId) {
        logger.debug("点赞文档请求: {}", documentId);

        try {
            boolean success = documentService.likeDocument(documentId);
            
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "点赞成功"
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "点赞失败，文档不存在"
                ));
            }
        } catch (Exception e) {
            logger.error("点赞文档时发生错误", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "服务器内部错误"
            ));
        }
    }

    /**
     * 根据分类查找文档
     * 
     * @param categoryId 分类ID
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 文档分页结果
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<?> getDocumentsByCategory(@PathVariable Long categoryId,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        logger.debug("根据分类查找文档: {}, page: {}, size: {}", categoryId, page, size);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<KnowledgeDocument> documents = documentService.findByCategory(categoryId, pageable);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", documents
            ));
        } catch (IllegalArgumentException e) {
            logger.warn("查找文档失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("查找文档时发生错误", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "服务器内部错误"
            ));
        }
    }

    /**
     * 根据状态查找文档
     * 
     * @param status 文档状态
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 文档分页结果
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<?> getDocumentsByStatus(@PathVariable String status,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        logger.debug("根据状态查找文档: {}, page: {}, size: {}", status, page, size);

        try {
            KnowledgeDocument.DocumentStatus documentStatus = KnowledgeDocument.DocumentStatus.valueOf(status.toUpperCase());
            Pageable pageable = PageRequest.of(page, size);
            Page<KnowledgeDocument> documents = documentService.findByStatus(documentStatus, pageable);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", documents
            ));
        } catch (IllegalArgumentException e) {
            logger.warn("查找文档失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "无效的文档状态: " + status
            ));
        } catch (Exception e) {
            logger.error("查找文档时发生错误", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "服务器内部错误"
            ));
        }
    }

    /**
     * 获取热门文档
     * 
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 热门文档分页结果
     */
    @GetMapping("/popular")
    public ResponseEntity<?> getPopularDocuments(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        logger.debug("获取热门文档: page: {}, size: {}", page, size);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<KnowledgeDocument> documents = documentService.getPopularDocuments(pageable);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", documents
            ));
        } catch (Exception e) {
            logger.error("获取热门文档时发生错误", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "服务器内部错误"
            ));
        }
    }

    /**
     * 获取最新文档
     *
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 最新文档分页结果
     */
    @GetMapping("/latest")
    public ResponseEntity<?> getLatestDocuments(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        logger.debug("获取最新文档: page: {}, size: {}", page, size);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<KnowledgeDocument> documents = documentService.getLatestDocuments(pageable);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", documents
            ));
        } catch (Exception e) {
            logger.error("获取最新文档时发生错误", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "服务器内部错误"
            ));
        }
    }

    /**
     * 搜索文档
     *
     * @param keyword 搜索关键词
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @return 搜索结果分页
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchDocuments(@RequestParam(required = false) String keyword,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        logger.debug("搜索文档: {}, page: {}, size: {}", keyword, page, size);

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<KnowledgeDocument> documents = documentService.searchDocuments(keyword, pageable);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", documents
            ));
        } catch (Exception e) {
            logger.error("搜索文档时发生错误", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "服务器内部错误"
            ));
        }
    }

    /**
     * 智能搜索文档（混合搜索）
     *
     * @param query 查询文本
     * @param maxResults 最大结果数
     * @return 搜索结果列表
     */
    @PostMapping("/search/smart")
    public ResponseEntity<?> smartSearch(@RequestParam String query,
                                       @RequestParam(defaultValue = "10") int maxResults) {
        logger.debug("智能搜索文档: {}, maxResults: {}", query, maxResults);

        try {
            List<KnowledgeDocument> documents = searchService.hybridSearch(query, maxResults);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", documents
            ));
        } catch (Exception e) {
            logger.error("智能搜索文档时发生错误", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "服务器内部错误"
            ));
        }
    }

    /**
     * 获取相关文档
     *
     * @param documentId 当前文档ID
     * @param maxResults 最大结果数
     * @return 相关文档列表
     */
    @GetMapping("/{documentId}/related")
    public ResponseEntity<?> getRelatedDocuments(@PathVariable Long documentId,
                                               @RequestParam(defaultValue = "5") int maxResults) {
        logger.debug("获取相关文档: {}, maxResults: {}", documentId, maxResults);

        try {
            List<KnowledgeDocument> documents = searchService.findRelatedDocuments(documentId, maxResults);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", documents
            ));
        } catch (Exception e) {
            logger.error("获取相关文档时发生错误", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "服务器内部错误"
            ));
        }
    }
}
