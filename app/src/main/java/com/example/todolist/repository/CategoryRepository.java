package com.example.todolist.repository;

import android.content.Context;

import com.example.todolist.database.ToDoDatabase;
import com.example.todolist.database.dao.CategoryDao;
import com.example.todolist.database.entity.CategoryEntity;
import com.example.todolist.database.mapper.CategoryMapper;
import com.example.todolist.model.Category;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class CategoryRepository extends BaseRepository {
    
    private CategoryDao categoryDao;
    
    public CategoryRepository(Context context) {
        super();
        ToDoDatabase database = ToDoDatabase.getInstance(context);
        categoryDao = database.categoryDao();
    }
    
    // === CRUD OPERATIONS ===
    
    public void addCategory(Category category, DatabaseCallback<String> callback) {
        executeAsync(() -> {
            try {
                // Generate ID if not set
                if (category.getId() == null || category.getId().isEmpty()) {
                    category.setId(UUID.randomUUID().toString());
                }
                
                // Set timestamps
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String currentDate = dateFormat.format(new Date());
                category.setCreatedAt(currentDate);
                category.setUpdatedAt(currentDate);
                
                CategoryEntity entity = CategoryMapper.toEntity(category);
                categoryDao.insertCategory(entity);
                runOnMainThread(() -> callback.onSuccess(entity.id));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi thêm danh mục: " + e.getMessage()));
            }
        });
    }
    
    public void updateCategory(Category category, DatabaseCallback<Boolean> callback) {
        executeAsync(() -> {
            try {
                // Update timestamp
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String currentDate = dateFormat.format(new Date());
                category.setUpdatedAt(currentDate);
                
                CategoryEntity entity = CategoryMapper.toEntity(category);
                categoryDao.updateCategory(entity);
                runOnMainThread(() -> callback.onSuccess(true));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi cập nhật danh mục: " + e.getMessage()));
            }
        });
    }
    
    public void deleteCategory(Category category, DatabaseCallback<Boolean> callback) {
        executeAsync(() -> {
            try {
                CategoryEntity entity = CategoryMapper.toEntity(category);
                categoryDao.deleteCategory(entity);
                runOnMainThread(() -> callback.onSuccess(true));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi xóa danh mục: " + e.getMessage()));
            }
        });
    }
    
    public void getCategoryById(String categoryId, RepositoryCallback<Category> callback) {
        executeAsync(() -> {
            try {
                CategoryEntity entity = categoryDao.getCategoryById(categoryId);
                Category category = CategoryMapper.fromEntity(entity);
                runOnMainThread(() -> callback.onSuccess(category));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi lấy danh mục: " + e.getMessage()));
            }
        });
    }
    
    // === QUERY OPERATIONS ===
    
    public void getAllCategories(ListCallback<Category> callback) {
        executeAsync(() -> {
            try {
                List<CategoryEntity> entities = categoryDao.getAllCategories();
                List<Category> categories = CategoryMapper.fromEntities(entities);
                runOnMainThread(() -> callback.onSuccess(categories));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi lấy danh sách danh mục: " + e.getMessage()));
            }
        });
    }
    
    public void searchCategories(String query, RepositoryCallback<List<Category>> callback) {
        executeAsync(() -> {
            try {
                List<CategoryEntity> entities = categoryDao.searchCategories(query);
                List<Category> categories = CategoryMapper.fromEntities(entities);
                runOnMainThread(() -> callback.onSuccess(categories));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi tìm kiếm danh mục: " + e.getMessage()));
            }
        });
    }
    
    public void getCategoriesByColor(String color, ListCallback<Category> callback) {
        executeAsync(() -> {
            try {
                List<CategoryEntity> entities = categoryDao.getCategoriesByColor(color);
                List<Category> categories = CategoryMapper.fromEntities(entities);
                runOnMainThread(() -> callback.onSuccess(categories));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi lấy danh mục theo màu: " + e.getMessage()));
            }
        });
    }
    
    public void getDefaultCategories(ListCallback<Category> callback) {
        executeAsync(() -> {
            try {
                List<CategoryEntity> entities = categoryDao.getDefaultCategories();
                List<Category> categories = CategoryMapper.fromEntities(entities);
                runOnMainThread(() -> callback.onSuccess(categories));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi lấy danh mục mặc định: " + e.getMessage()));
            }
        });
    }
    
    // === INITIALIZATION OPERATIONS ===
    
    public void initializeDefaultCategories(DatabaseCallback<Boolean> callback) {
        executeAsync(() -> {
            try {
                // Check if default categories already exist
                List<CategoryEntity> existing = categoryDao.getDefaultCategories();
                if (!existing.isEmpty()) {
                    runOnMainThread(() -> callback.onSuccess(true));
                    return;
                }
                
                // Create default categories
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String currentDate = dateFormat.format(new Date());
                
                Category[] defaultCategories = {
                    createDefaultCategory("Công việc", "#FF5722", "work", 1, currentDate),
                    createDefaultCategory("Cá nhân", "#2196F3", "personal", 2, currentDate),
                    createDefaultCategory("Mua sắm", "#4CAF50", "shopping", 3, currentDate),
                    createDefaultCategory("Sức khỏe", "#E91E63", "health", 4, currentDate),
                    createDefaultCategory("Học tập", "#9C27B0", "study", 5, currentDate)
                };
                
                for (Category category : defaultCategories) {
                    CategoryEntity entity = CategoryMapper.toEntity(category);
                    categoryDao.insertCategory(entity);
                }
                
                runOnMainThread(() -> callback.onSuccess(true));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi khởi tạo danh mục mặc định: " + e.getMessage()));
            }
        });
    }
    
    public void removeDuplicateCategories(DatabaseCallback<Boolean> callback) {
        executeAsync(() -> {
            try {
                // Get all categories
                List<CategoryEntity> allCategories = categoryDao.getAllCategories();
                
                // Remove duplicates based on name (keep the first occurrence)
                for (int i = 0; i < allCategories.size(); i++) {
                    CategoryEntity current = allCategories.get(i);
                    for (int j = i + 1; j < allCategories.size(); j++) {
                        CategoryEntity other = allCategories.get(j);
                        if (current.name.equals(other.name)) {
                            categoryDao.deleteCategory(other);
                            allCategories.remove(j);
                            j--; // Adjust index after removal
                        }
                    }
                }
                
                runOnMainThread(() -> callback.onSuccess(true));
            } catch (Exception e) {
                runOnMainThread(() -> callback.onError("Lỗi xóa danh mục trùng lặp: " + e.getMessage()));
            }
        });
    }
    
    private Category createDefaultCategory(String name, String color, String icon, int sortOrder, String currentDate) {
        Category category = new Category();
        category.setId(UUID.randomUUID().toString());
        category.setName(name);
        category.setColor(color);
        category.setIcon(icon);
        category.setSortOrder(sortOrder);
        category.setDefault(true);
        category.setCreatedAt(currentDate);
        category.setUpdatedAt(currentDate);
        return category;
    }

    public void deleteAllCategories(DatabaseCallback<Boolean> callback) {
        executeAsync(() -> {
            try {
                categoryDao.deleteAllCategories();
                callback.onSuccess(true);
            } catch (Exception e) {
                callback.onError("Failed to delete all categories: " + e.getMessage());
            }
        });
    }
}
