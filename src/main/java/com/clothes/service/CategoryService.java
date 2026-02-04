package com.clothes.service;

import com.clothes.dao.ProductDAO;
import com.clothes.dao.CategoryDAO;
import com.clothes.model.Category;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for Category management
 */
@Service
public class CategoryService {

    private final CategoryDAO categoryDAO;

    public CategoryService(CategoryDAO categoryDAO) {
        this.categoryDAO = categoryDAO;
    }

    /**
     * Create a new category
     */
    public Long createCategory(String categoryName, String description, Long parentId) {
        Category category = new Category();
        category.setCategoryName(categoryName);
        category.setDescription(description);
        category.setParentId(parentId);

        return categoryDAO.save(category);
    }

    /**
     * Create a new category with display order
     */
    public Long createCategory(String categoryName, String description, Long parentId, Integer displayOrder) {
        Category category = new Category();
        category.setCategoryName(categoryName);
        category.setDescription(description);
        category.setParentId(parentId);

        Long categoryId = categoryDAO.save(category);

        if (displayOrder != null && displayOrder > 0) {
            categoryDAO.updateDisplayOrder(categoryId, displayOrder);
        }

        return categoryId;
    }

    /**
     * Update category
     */
    public boolean updateCategory(Long categoryId, String categoryName, String description, Long parentId) {
        Optional<Category> categoryOpt = categoryDAO.findById(categoryId);
        if (categoryOpt.isEmpty()) {
            return false;
        }

        Category category = categoryOpt.get();
        category.setCategoryName(categoryName);
        category.setDescription(description);
        category.setParentId(parentId);

        return categoryDAO.update(category) > 0;
    }

    /**
     * Update category with display order
     */
    public boolean updateCategory(Long categoryId, String categoryName, String description, Long parentId,
            Integer displayOrder) {
        Optional<Category> categoryOpt = categoryDAO.findById(categoryId);
        if (categoryOpt.isEmpty()) {
            return false;
        }

        Category category = categoryOpt.get();
        category.setCategoryName(categoryName);
        category.setDescription(description);
        category.setParentId(parentId);

        boolean updated = categoryDAO.update(category) > 0;

        if (updated && displayOrder != null && displayOrder > 0) {
            categoryDAO.updateDisplayOrder(categoryId, displayOrder);
        }

        return updated;
    }

    /**
     * Get category by ID
     */
    public Optional<Category> getCategoryById(Long categoryId) {
        return categoryDAO.findById(categoryId);
    }

    /**
     * Get all categories
     */
    public List<Category> getAllCategories() {
        return categoryDAO.findAll();
    }

    /**
     * Get root categories (no parent)
     */
    public List<Category> getRootCategories() {
        return categoryDAO.findRootCategories();
    }

    /**
     * Get child categories by parent ID
     */
    public List<Category> getChildCategories(Long parentId) {
        return categoryDAO.findByParentId(parentId);
    }

    /**
     * Delete category
     */
    public boolean deleteCategory(Long categoryId) {
        // Check if category has children
        List<Category> children = categoryDAO.findByParentId(categoryId);
        if (!children.isEmpty()) {
            throw new IllegalArgumentException("Không thể xóa danh mục có danh mục con");
        }

        return categoryDAO.delete(categoryId) > 0;
    }

    /**
     * Get total category count
     */
    public int getTotalCategories() {
        return categoryDAO.count();
    }

    /**
     * Get product count for a category
     */
    public int getProductCountByCategory(Long categoryId) {
        return categoryDAO.getProductCount(categoryId);
    }

    /**
     * Update display order
     */
    public boolean updateDisplayOrder(Long categoryId, int displayOrder) {
        return categoryDAO.updateDisplayOrder(categoryId, displayOrder) > 0;
    }

    /**
     * Check if category has products
     */
    public boolean hasProducts(Long categoryId) {
        return categoryDAO.getProductCount(categoryId) > 0;
    }

    /**
     * Save or update category for admin
     */
    public Category saveCategory(Category category) {
        if (category.getCategoryId() == null) {
            Long id = categoryDAO.save(category);
            category.setCategoryId(id);
        } else {
            categoryDAO.update(category);
        }
        return category;
    }

    /**
     * Toggle category active status
     */
    public void toggleCategoryStatus(Long categoryId) {
        Optional<Category> category = categoryDAO.findById(categoryId);
        category.ifPresent(c -> {
            c.setIsActive(!Boolean.TRUE.equals(c.getIsActive()));
            categoryDAO.update(c);
        });
    }

    /**
     * Get categories with product counts
     */
    public List<Category> getCategoriesWithProductCount() {
        List<Category> categories = categoryDAO.findAll();
        categories.forEach(c -> {
            c.setProductCount(categoryDAO.getProductCount(c.getCategoryId()));
            if (c.getParentId() != null) {
                categoryDAO.findById(c.getParentId()).ifPresent(c::setParentCategory);
            }
        });
        return categories;
    }
}
