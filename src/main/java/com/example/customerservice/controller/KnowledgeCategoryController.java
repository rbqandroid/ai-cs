package com.example.customerservice.controller;

import com.example.customerservice.entity.KnowledgeCategory;
import com.example.customerservice.service.KnowledgeCategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 知识分类管理控制器
 * 
 * 提供知识分类的REST API接口，包括分类的创建、查询、更新、删除等功能。
 * 支持层级分类管理和分类树的构建。
 * 
 * @author AI Assistant
 * @version 1.0.0
 * @since 2025-07-06
 */
@RestController
@RequestMapping("/api/knowledge/categories")
@CrossOrigin(origins = "*") // 允许跨域访问，生产环境应该限制具体域名
public class KnowledgeCategoryController {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeCategoryController.class);

    @Autowired
    private KnowledgeCategoryService categoryService;

    /**
     * 创建新的知识分类
     * 
     * @param category 分类信息
     * @return 创建的分类对象
     */
    @PostMapping
    public ResponseEntity<?> createCategory(@Valid @RequestBody KnowledgeCategory category) {
        logger.info("创建知识分类请求: {}", category.getName());

        try {
            // 从请求头或认证信息中获取创建者，这里暂时使用固定值
            String createdBy = "admin"; // TODO: 从认证信息中获取

            KnowledgeCategory createdCategory = categoryService.createCategory(category, createdBy);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "分类创建成功",
                "data", createdCategory
            ));
        } catch (IllegalArgumentException e) {
            logger.warn("创建分类失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("创建分类时发生错误", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "服务器内部错误"
            ));
        }
    }

    /**
     * 获取所有顶级分类
     * 
     * @return 顶级分类列表
     */
    @GetMapping("/top-level")
    public ResponseEntity<?> getTopLevelCategories() {
        logger.debug("获取顶级分类列表");

        try {
            List<KnowledgeCategory> categories = categoryService.getTopLevelCategories();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", categories
            ));
        } catch (Exception e) {
            logger.error("获取顶级分类时发生错误", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "服务器内部错误"
            ));
        }
    }

    /**
     * 获取指定分类的子分类
     * 
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    @GetMapping("/{parentId}/children")
    public ResponseEntity<?> getChildCategories(@PathVariable Long parentId) {
        logger.debug("获取子分类列表: {}", parentId);

        try {
            List<KnowledgeCategory> children = categoryService.getChildCategories(parentId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", children
            ));
        } catch (Exception e) {
            logger.error("获取子分类时发生错误", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "服务器内部错误"
            ));
        }
    }

    /**
     * 根据ID获取分类详情
     * 
     * @param categoryId 分类ID
     * @return 分类详情
     */
    @GetMapping("/{categoryId}")
    public ResponseEntity<?> getCategoryById(@PathVariable Long categoryId) {
        logger.debug("获取分类详情: {}", categoryId);

        try {
            Optional<KnowledgeCategory> categoryOpt = categoryService.findById(categoryId);
            
            if (categoryOpt.isPresent()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", categoryOpt.get()
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("获取分类详情时发生错误", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "服务器内部错误"
            ));
        }
    }

    /**
     * 更新分类信息
     * 
     * @param categoryId 分类ID
     * @param updatedCategory 更新的分类信息
     * @return 更新后的分类对象
     */
    @PutMapping("/{categoryId}")
    public ResponseEntity<?> updateCategory(@PathVariable Long categoryId, 
                                          @Valid @RequestBody KnowledgeCategory updatedCategory) {
        logger.info("更新分类请求: {}", categoryId);

        try {
            // 从请求头或认证信息中获取更新者，这里暂时使用固定值
            String updatedBy = "admin"; // TODO: 从认证信息中获取

            KnowledgeCategory category = categoryService.updateCategory(categoryId, updatedCategory, updatedBy);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "分类更新成功",
                "data", category
            ));
        } catch (IllegalArgumentException e) {
            logger.warn("更新分类失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("更新分类时发生错误", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "服务器内部错误"
            ));
        }
    }

    /**
     * 删除分类
     * 
     * @param categoryId 分类ID
     * @return 删除结果
     */
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long categoryId) {
        logger.info("删除分类请求: {}", categoryId);

        try {
            categoryService.deleteCategory(categoryId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "分类删除成功"
            ));
        } catch (IllegalArgumentException e) {
            logger.warn("删除分类失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("删除分类时发生错误", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "服务器内部错误"
            ));
        }
    }

    /**
     * 搜索分类
     * 
     * @param keyword 搜索关键词
     * @return 匹配的分类列表
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchCategories(@RequestParam(required = false) String keyword) {
        logger.debug("搜索分类: {}", keyword);

        try {
            List<KnowledgeCategory> categories = categoryService.searchCategories(keyword);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", categories
            ));
        } catch (Exception e) {
            logger.error("搜索分类时发生错误", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "服务器内部错误"
            ));
        }
    }

    /**
     * 获取激活状态的分类
     * 
     * @return 激活分类列表
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveCategories() {
        logger.debug("获取激活分类列表");

        try {
            List<KnowledgeCategory> categories = categoryService.getActiveCategories();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", categories
            ));
        } catch (Exception e) {
            logger.error("获取激活分类时发生错误", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "服务器内部错误"
            ));
        }
    }

    /**
     * 获取分类层级结构
     * 
     * @param categoryId 分类ID
     * @return 分类及其祖先分类列表
     */
    @GetMapping("/{categoryId}/hierarchy")
    public ResponseEntity<?> getCategoryHierarchy(@PathVariable Long categoryId) {
        logger.debug("获取分类层级结构: {}", categoryId);

        try {
            List<KnowledgeCategory> hierarchy = categoryService.getCategoryHierarchy(categoryId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", hierarchy
            ));
        } catch (Exception e) {
            logger.error("获取分类层级结构时发生错误", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "服务器内部错误"
            ));
        }
    }

    /**
     * 获取分类的所有后代分类
     * 
     * @param categoryId 分类ID
     * @return 所有后代分类列表
     */
    @GetMapping("/{categoryId}/descendants")
    public ResponseEntity<?> getAllDescendants(@PathVariable Long categoryId) {
        logger.debug("获取所有后代分类: {}", categoryId);

        try {
            List<KnowledgeCategory> descendants = categoryService.getAllDescendants(categoryId);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", descendants
            ));
        } catch (IllegalArgumentException e) {
            logger.warn("获取后代分类失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("获取后代分类时发生错误", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "服务器内部错误"
            ));
        }
    }
}
