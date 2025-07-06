package com.example.customerservice.service;

import com.example.customerservice.entity.KnowledgeCategory;
import com.example.customerservice.repository.KnowledgeCategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 知识分类服务类
 * 
 * 提供知识分类的业务逻辑处理，包括分类的创建、更新、删除、查询等功能。
 * 支持层级分类管理和分类树的构建。
 * 
 * @author AI Assistant
 * @version 1.0.0
 * @since 2025-07-06
 */
@Service
@Transactional
public class KnowledgeCategoryService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeCategoryService.class);

    @Autowired
    private KnowledgeCategoryRepository categoryRepository;

    /**
     * 创建新的知识分类
     * 
     * @param category 分类对象
     * @param createdBy 创建者
     * @return 创建的分类对象
     * @throws IllegalArgumentException 当分类信息无效时抛出
     */
    public KnowledgeCategory createCategory(KnowledgeCategory category, String createdBy) {
        logger.info("创建知识分类: {}, 创建者: {}", category.getName(), createdBy);

        // 验证分类信息
        validateCategory(category);

        // 检查同一父分类下是否存在重名分类
        if (categoryRepository.existsByNameAndParentIdAndIdNot(
                category.getName(), category.getParentId(), null)) {
            throw new IllegalArgumentException("同一父分类下已存在相同名称的分类");
        }

        // 设置创建者
        category.setCreatedBy(createdBy);

        // 设置层级和路径
        setupCategoryHierarchy(category);

        // 设置排序权重
        if (category.getSortOrder() == null) {
            Integer maxOrder = categoryRepository.findMaxSortOrderByParentId(category.getParentId());
            category.setSortOrder(maxOrder + 1);
        }

        KnowledgeCategory savedCategory = categoryRepository.save(category);
        logger.info("成功创建知识分类: {}, ID: {}", savedCategory.getName(), savedCategory.getId());

        return savedCategory;
    }

    /**
     * 更新知识分类
     * 
     * @param categoryId 分类ID
     * @param updatedCategory 更新的分类信息
     * @param updatedBy 更新者
     * @return 更新后的分类对象
     * @throws IllegalArgumentException 当分类不存在或信息无效时抛出
     */
    public KnowledgeCategory updateCategory(Long categoryId, KnowledgeCategory updatedCategory, String updatedBy) {
        logger.info("更新知识分类: {}, 更新者: {}", categoryId, updatedBy);

        KnowledgeCategory existingCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在: " + categoryId));

        // 验证更新信息
        validateCategory(updatedCategory);

        // 检查重名（排除当前分类）
        if (categoryRepository.existsByNameAndParentIdAndIdNot(
                updatedCategory.getName(), updatedCategory.getParentId(), categoryId)) {
            throw new IllegalArgumentException("同一父分类下已存在相同名称的分类");
        }

        // 更新基本信息
        existingCategory.setName(updatedCategory.getName());
        existingCategory.setDescription(updatedCategory.getDescription());
        existingCategory.setStatus(updatedCategory.getStatus());
        existingCategory.setSortOrder(updatedCategory.getSortOrder());

        // 如果父分类发生变化，需要重新设置层级和路径
        if (!java.util.Objects.equals(existingCategory.getParentId(), updatedCategory.getParentId())) {
            existingCategory.setParentId(updatedCategory.getParentId());
            setupCategoryHierarchy(existingCategory);
            
            // 同时更新所有子分类的层级和路径
            updateChildrenHierarchy(existingCategory);
        }

        KnowledgeCategory savedCategory = categoryRepository.save(existingCategory);
        logger.info("成功更新知识分类: {}", savedCategory.getId());

        return savedCategory;
    }

    /**
     * 删除知识分类
     * 
     * @param categoryId 分类ID
     * @throws IllegalArgumentException 当分类不存在或包含子分类/文档时抛出
     */
    public void deleteCategory(Long categoryId) {
        logger.info("删除知识分类: {}", categoryId);

        KnowledgeCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在: " + categoryId));

        // 检查是否有子分类
        long childCount = categoryRepository.countByParentId(categoryId);
        if (childCount > 0) {
            throw new IllegalArgumentException("无法删除包含子分类的分类，请先删除或移动子分类");
        }

        // 检查是否有关联文档
        long documentCount = categoryRepository.countDocumentsByCategoryId(categoryId);
        if (documentCount > 0) {
            throw new IllegalArgumentException("无法删除包含文档的分类，请先删除或移动文档");
        }

        categoryRepository.delete(category);
        logger.info("成功删除知识分类: {}", categoryId);
    }

    /**
     * 根据ID查找分类
     * 
     * @param categoryId 分类ID
     * @return 分类对象
     */
    @Transactional(readOnly = true)
    public Optional<KnowledgeCategory> findById(Long categoryId) {
        return categoryRepository.findById(categoryId);
    }

    /**
     * 根据名称查找分类
     * 
     * @param name 分类名称
     * @return 分类对象
     */
    @Transactional(readOnly = true)
    public Optional<KnowledgeCategory> findByName(String name) {
        return categoryRepository.findByName(name);
    }

    /**
     * 获取所有顶级分类
     * 
     * @return 顶级分类列表
     */
    @Transactional(readOnly = true)
    public List<KnowledgeCategory> getTopLevelCategories() {
        return categoryRepository.findByParentIdIsNullOrderBySortOrderAsc();
    }

    /**
     * 获取指定分类的子分类
     * 
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    @Transactional(readOnly = true)
    public List<KnowledgeCategory> getChildCategories(Long parentId) {
        return categoryRepository.findByParentIdOrderBySortOrderAsc(parentId);
    }

    /**
     * 获取激活状态的分类
     * 
     * @return 激活分类列表
     */
    @Transactional(readOnly = true)
    public List<KnowledgeCategory> getActiveCategories() {
        return categoryRepository.findByStatusOrderBySortOrderAsc(KnowledgeCategory.CategoryStatus.ACTIVE);
    }

    /**
     * 搜索分类
     * 
     * @param keyword 搜索关键词
     * @return 匹配的分类列表
     */
    @Transactional(readOnly = true)
    public List<KnowledgeCategory> searchCategories(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getActiveCategories();
        }
        return categoryRepository.searchByKeyword(keyword.trim());
    }

    /**
     * 获取分类层级结构
     * 
     * @param categoryId 分类ID
     * @return 分类及其祖先分类列表
     */
    @Transactional(readOnly = true)
    public List<KnowledgeCategory> getCategoryHierarchy(Long categoryId) {
        return categoryRepository.findCategoryHierarchy(categoryId);
    }

    /**
     * 获取分类的所有后代分类
     * 
     * @param categoryId 分类ID
     * @return 所有后代分类列表
     */
    @Transactional(readOnly = true)
    public List<KnowledgeCategory> getAllDescendants(Long categoryId) {
        KnowledgeCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("分类不存在: " + categoryId));
        
        return categoryRepository.findAllDescendants(category.getCategoryPath());
    }

    /**
     * 验证分类信息
     * 
     * @param category 分类对象
     * @throws IllegalArgumentException 当分类信息无效时抛出
     */
    private void validateCategory(KnowledgeCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("分类对象不能为空");
        }

        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("分类名称不能为空");
        }

        if (category.getName().length() > 100) {
            throw new IllegalArgumentException("分类名称长度不能超过100字符");
        }

        if (category.getDescription() != null && category.getDescription().length() > 500) {
            throw new IllegalArgumentException("分类描述长度不能超过500字符");
        }
    }

    /**
     * 设置分类的层级和路径
     *
     * @param category 分类对象
     */
    private void setupCategoryHierarchy(KnowledgeCategory category) {
        if (category.getParentId() == null) {
            // 顶级分类
            category.setLevel(0);
            // 路径将在保存后设置，因为需要ID
        } else {
            // 子分类
            KnowledgeCategory parent = categoryRepository.findById(category.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("父分类不存在: " + category.getParentId()));

            category.setLevel(parent.getLevel() + 1);
            // 路径将在保存后设置，因为需要ID
        }
    }

    /**
     * 更新子分类的层级和路径
     * 
     * @param parentCategory 父分类
     */
    private void updateChildrenHierarchy(KnowledgeCategory parentCategory) {
        List<KnowledgeCategory> children = categoryRepository.findByParentIdOrderBySortOrderAsc(parentCategory.getId());
        
        for (KnowledgeCategory child : children) {
            child.setLevel(parentCategory.getLevel() + 1);
            child.setCategoryPath(parentCategory.getCategoryPath() + "/" + child.getId());
            categoryRepository.save(child);
            
            // 递归更新子分类的子分类
            updateChildrenHierarchy(child);
        }
    }
}
