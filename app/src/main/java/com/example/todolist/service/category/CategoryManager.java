package com.example.todolist.service.category;

import android.content.Context;
import com.example.todolist.model.Category;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.repository.CategoryRepository;

import java.util.List;

/**
 * CategoryManager - Xử lý các thao tác CRUD cho Category
 */
public class CategoryManager {
    
    private Context context;
    private CategoryRepository categoryRepository;

    public CategoryManager(Context context) {
        this.context = context;
        this.categoryRepository = new CategoryRepository(context);
    }

    public void addCategory(Category category, BaseRepository.DatabaseCallback<String> callback) {
        categoryRepository.addCategory(category, callback);
    }

    public void updateCategory(Category category, BaseRepository.DatabaseCallback<Boolean> callback) {
        categoryRepository.updateCategory(category, callback);
    }

    public void deleteCategory(Category category, BaseRepository.DatabaseCallback<Boolean> callback) {
        categoryRepository.deleteCategory(category, callback);
    }

    public void getCategoryById(String categoryId, BaseRepository.RepositoryCallback<Category> callback) {
        categoryRepository.getCategoryById(categoryId, callback);
    }

    public void getAllCategories(BaseRepository.ListCallback<Category> callback) {
        categoryRepository.getAllCategories(callback);
    }
    
    public List<Category> getCategories() {
        // For synchronous access, return empty list and recommend using getAllCategories() callback
        return new java.util.ArrayList<>();
    }

    public void initializeDefaultCategories(BaseRepository.DatabaseCallback<Boolean> callback) {
        categoryRepository.initializeDefaultCategories(callback);
    }

    public void removeDuplicateCategories(BaseRepository.DatabaseCallback<Boolean> callback) {
        categoryRepository.removeDuplicateCategories(callback);
    }

    public void searchCategories(String query, BaseRepository.RepositoryCallback<List<Category>> callback) {
        categoryRepository.searchCategories(query, callback);
    }

    public void validateCategoryName(String name, String currentCategoryId, BaseRepository.RepositoryCallback<Boolean> callback) {
        categoryRepository.getAllCategories(new BaseRepository.ListCallback<Category>() {
            @Override
            public void onSuccess(List<Category> categories) {
                boolean isValid = true;
                
                for (Category category : categories) {
                    if (category.getName().toLowerCase().equals(name.toLowerCase()) &&
                        (currentCategoryId == null || !category.getId().equals(currentCategoryId))) {
                        isValid = false;
                        break;
                    }
                }
                
                callback.onSuccess(isValid);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public Category createDefaultCategory(String name, String color, String icon) {
        Category category = new Category();
        category.setName(name);
        category.setColor(color);
        category.setIcon(icon);
        return category;
    }

    public boolean isDefaultCategory(Category category) {
        if (category == null || category.getName() == null) return false;
        
        String name = category.getName().toLowerCase();
        return name.equals("cá nhân") || 
               name.equals("yêu thích") || 
               name.equals("công việc") ||
               name.equals("personal") ||
               name.equals("favorite") ||
               name.equals("work") ||
               name.equals("học tập") ||
               name.equals("study");
    }
}
