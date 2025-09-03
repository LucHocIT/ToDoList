package com.example.todolist.service;

import android.content.Context;
import com.example.todolist.model.Category;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.repository.CategoryRepository;
import com.example.todolist.service.category.CategoryManager;

import java.util.List;

public class CategoryService {
    
    public interface CategoryUpdateListener {
        void onCategoriesUpdated(List<Category> categories);
        void onError(String error);
    }
    
    public interface CategoryOperationCallback {
        void onSuccess();
        void onError(String error);
    }
    
    private Context context;
    private CategoryRepository categoryRepository;
    private CategoryUpdateListener listener;
    private CategoryManager categoryManager;
    
    public CategoryService(Context context, CategoryUpdateListener listener) {
        this.context = context;
        this.listener = listener;
        this.categoryRepository = new CategoryRepository(context);
        this.categoryManager = new CategoryManager(context);
    }
    
    public void loadCategories() {
        categoryRepository.getAllCategories(new BaseRepository.ListCallback<Category>() {
            @Override
            public void onSuccess(List<Category> categories) {
                if (listener != null) {
                    listener.onCategoriesUpdated(categories);
                }
            }
            
            @Override
            public void onError(String error) {
                if (listener != null) {
                    listener.onError(error);
                }
            }
        });
    }
    
    public List<Category> getCategories() {
        return categoryManager.getCategories();
    }
    
    public void getAllCategories(BaseRepository.ListCallback<Category> callback) {
        categoryRepository.getAllCategories(callback);
    }
    
    public void createCategory(Category category, CategoryOperationCallback callback) {
        categoryRepository.addCategory(category, new BaseRepository.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                if (callback != null) {
                    callback.onSuccess();
                }
            }
            
            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }
    
    public void updateCategory(Category category, CategoryOperationCallback callback) {
        categoryRepository.updateCategory(category, new BaseRepository.DatabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (callback != null) {
                    callback.onSuccess();
                }
            }
            
            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }
    
    public void deleteCategory(Category category, CategoryOperationCallback callback) {
        categoryRepository.deleteCategory(category, new BaseRepository.DatabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (callback != null) {
                    callback.onSuccess();
                }
            }
            
            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }
    
    public void clearAllCategories(CategoryOperationCallback callback) {
        categoryRepository.deleteAllCategories(new BaseRepository.DatabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (callback != null) {
                    callback.onSuccess();
                }
            }
            
            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }
    
    public void initializeDefaultCategories(CategoryOperationCallback callback) {
        categoryRepository.initializeDefaultCategories(new BaseRepository.DatabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (callback != null) {
                    callback.onSuccess();
                }
            }
            
            @Override
            public void onError(String error) {
                if (callback != null) {
                    callback.onError(error);
                }
            }
        });
    }
    
    public void cleanup() {
        // Clean up resources if needed
    }
}
