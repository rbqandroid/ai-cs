package com.example.customerservice.repository;

import com.example.customerservice.entity.KnowledgeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 知识分类数据访问层接口
 * 
 * 提供知识分类的数据库操作方法，包括基本的CRUD操作和自定义查询方法。
 * 支持层级分类的查询和管理功能。
 * 
 * @author AI Assistant
 * @version 1.0.0
 * @since 2025-07-06
 */
@Repository
public interface KnowledgeCategoryRepository extends JpaRepository<KnowledgeCategory, Long> {

    /**
     * 根据分类名称查找分类
     * 
     * @param name 分类名称
     * @return 分类对象，如果不存在则返回空
     */
    Optional<KnowledgeCategory> findByName(String name);

    /**
     * 根据父分类ID查找所有子分类
     * 按排序权重升序排列
     * 
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    List<KnowledgeCategory> findByParentIdOrderBySortOrderAsc(Long parentId);

    /**
     * 查找所有顶级分类（父分类ID为null）
     * 按排序权重升序排列
     * 
     * @return 顶级分类列表
     */
    List<KnowledgeCategory> findByParentIdIsNullOrderBySortOrderAsc();

    /**
     * 根据分类状态查找分类
     * 按排序权重升序排列
     * 
     * @param status 分类状态
     * @return 分类列表
     */
    List<KnowledgeCategory> findByStatusOrderBySortOrderAsc(KnowledgeCategory.CategoryStatus status);

    /**
     * 根据分类层级查找分类
     * 
     * @param level 分类层级
     * @return 分类列表
     */
    List<KnowledgeCategory> findByLevel(Integer level);

    /**
     * 根据分类名称模糊查询
     * 
     * @param name 分类名称关键词
     * @return 匹配的分类列表
     */
    List<KnowledgeCategory> findByNameContainingIgnoreCase(String name);

    /**
     * 查找指定分类的所有子分类（包括子分类的子分类）
     * 使用递归查询
     * 
     * @param categoryPath 分类路径
     * @return 所有子分类列表
     */
    @Query("SELECT c FROM KnowledgeCategory c WHERE c.categoryPath LIKE CONCAT(:categoryPath, '%') AND c.categoryPath != :categoryPath")
    List<KnowledgeCategory> findAllDescendants(@Param("categoryPath") String categoryPath);

    /**
     * 统计指定父分类下的子分类数量
     * 
     * @param parentId 父分类ID
     * @return 子分类数量
     */
    long countByParentId(Long parentId);

    /**
     * 统计指定分类下的文档数量
     * 
     * @param categoryId 分类ID
     * @return 文档数量
     */
    @Query("SELECT COUNT(d) FROM KnowledgeDocument d WHERE d.category.id = :categoryId")
    long countDocumentsByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 查找包含指定关键词的分类
     * 在分类名称和描述中搜索
     * 
     * @param keyword 搜索关键词
     * @return 匹配的分类列表
     */
    @Query("SELECT c FROM KnowledgeCategory c WHERE " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<KnowledgeCategory> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 查找指定分类及其所有祖先分类
     * 
     * @param categoryId 分类ID
     * @return 分类及其祖先分类列表
     */
    @Query(value = "WITH RECURSIVE category_hierarchy AS (" +
                   "  SELECT id, name, parent_id, level, category_path " +
                   "  FROM knowledge_categories " +
                   "  WHERE id = :categoryId " +
                   "  UNION ALL " +
                   "  SELECT c.id, c.name, c.parent_id, c.level, c.category_path " +
                   "  FROM knowledge_categories c " +
                   "  INNER JOIN category_hierarchy ch ON c.id = ch.parent_id" +
                   ") " +
                   "SELECT * FROM category_hierarchy ORDER BY level", 
           nativeQuery = true)
    List<KnowledgeCategory> findCategoryHierarchy(@Param("categoryId") Long categoryId);

    /**
     * 检查分类名称在同一父分类下是否唯一
     * 
     * @param name 分类名称
     * @param parentId 父分类ID
     * @param excludeId 排除的分类ID（用于更新时检查）
     * @return 是否存在重复名称
     */
    @Query("SELECT COUNT(c) > 0 FROM KnowledgeCategory c WHERE " +
           "c.name = :name AND c.parentId = :parentId AND " +
           "(:excludeId IS NULL OR c.id != :excludeId)")
    boolean existsByNameAndParentIdAndIdNot(@Param("name") String name, 
                                           @Param("parentId") Long parentId, 
                                           @Param("excludeId") Long excludeId);

    /**
     * 查找最大排序权重
     * 用于在同一父分类下添加新分类时确定排序位置
     * 
     * @param parentId 父分类ID
     * @return 最大排序权重
     */
    @Query("SELECT COALESCE(MAX(c.sortOrder), 0) FROM KnowledgeCategory c WHERE c.parentId = :parentId")
    Integer findMaxSortOrderByParentId(@Param("parentId") Long parentId);

    /**
     * 批量更新分类状态
     * 
     * @param categoryIds 分类ID列表
     * @param status 新状态
     * @return 更新的记录数
     */
    @Query("UPDATE KnowledgeCategory c SET c.status = :status WHERE c.id IN :categoryIds")
    int updateStatusByIds(@Param("categoryIds") List<Long> categoryIds, 
                         @Param("status") KnowledgeCategory.CategoryStatus status);

    /**
     * 删除指定分类及其所有子分类
     * 注意：这是物理删除，请谨慎使用
     * 
     * @param categoryPath 分类路径
     * @return 删除的记录数
     */
    @Query("DELETE FROM KnowledgeCategory c WHERE c.categoryPath LIKE CONCAT(:categoryPath, '%')")
    int deleteByCategory(@Param("categoryPath") String categoryPath);
}
