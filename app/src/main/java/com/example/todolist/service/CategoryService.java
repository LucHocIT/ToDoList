package com.example.todolist.service;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.example.todolist.model.Category;
import com.example.todolist.repository.BaseRepository;
import com.example.todolist.repository.CategoryRepository;
import com.example.todolist.service.category.CategoryManager;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class CategoryService {
    
    public interface CategoryUpdateListener {
        void onCategoriesUpdated();
        void onError(String error);
    }

    public interface CategoryOperationCallback {
        void onSuccess();
        void onError(String error);
    }
    
    private Context context;
    private CategoryRepository categoryRepository;
    private CategoryUpdateListener listener;
    private ValueEventListener realtimeListener;

    private CategoryManager categoryManager;     // CRUD operations
    
    private List<Category> categories;

    public CategoryService(Context context, CategoryUpdateListener listener) {
        this.context = context;
        this.listener = listener;
        this.categoryRepository = new CategoryRepository();
        this.categories = new ArrayList<>();
        
        // Initialize sub-services
        this.categoryManager = new CategoryManager(context);
    }

    public void loadCategories() {
        // First clean up any duplicate categories
        categoryManager.removeDuplicateCategories(new BaseRepository.DatabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                // Initialize default categories after cleanup
                categoryManager.initializeDefaultCategories(new BaseRepository.DatabaseCallback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean initResult) {
                        setupRealtimeListener();
                    }

                    @Override
                    public void onError(String error) {
                        setupRealtimeListener();
                    }
                });
            }

            @Override
            public void onError(String error) {
                setupRealtimeListener();
            }
        });
    }

    private void setupRealtimeListener() {
        realtimeListener = categoryRepository.addCategoriesRealtimeListener(new BaseRepository.ListCallback<Category>() {
            @Override
            public void onSuccess(List<Category> categoriesList) {
                categories = categoriesList;
                
                if (listener != null) {
                    listener.onCategoriesUpdated();
                }
            }

            @Override
            public void onError(String error) {
                if (listener != null) {
                    listener.onError("Lỗi tải categories: " + error);
                }
            }
        });
    }

    // === CRUD OPERATIONS - Delegate to CategoryManager ===
    public void addCategory(Category category) {
        addCategory(category, null);
    }
    
    public void addCategory(Category category, CategoryOperationCallback callback) {
        categoryManager.addCategory(category, new BaseRepository.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String categoryId) {
                if (callback != null) callback.onSuccess();
                showToast("Thêm category thành công");
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
                showToast("Lỗi thêm category: " + error);
            }
        });
    }

    public void updateCategory(Category category) {
        updateCategory(category, null);
    }
    
    public void updateCategory(Category category, CategoryOperationCallback callback) {
        categoryManager.updateCategory(category, new BaseRepository.DatabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (callback != null) callback.onSuccess();
                showToast("Cập nhật category thành công");
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
                showToast("Lỗi cập nhật category: " + error);
            }
        });
    }

    public void deleteCategory(Category category) {
        deleteCategory(category, null);
    }
    
    public void deleteCategory(Category category, CategoryOperationCallback callback) {
        categoryManager.deleteCategory(category, new BaseRepository.DatabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (callback != null) callback.onSuccess();
                showToast("Xóa category thành công");
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
                showToast("Lỗi xóa category: " + error);
            }
        });
    }

    // === QUERY OPERATIONS - Delegate to CategoryManager ===
    public void getAllCategories(BaseRepository.ListCallback<Category> callback) {
        categoryManager.getAllCategories(callback);
    }

    public void getAllCategories(BaseRepository.RepositoryCallback<List<Category>> callback) {
        categoryManager.getAllCategories(new BaseRepository.ListCallback<Category>() {
            @Override
            public void onSuccess(List<Category> categories) {
                callback.onSuccess(categories);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    public void getCategoryById(String categoryId, BaseRepository.RepositoryCallback<Category> callback) {
        categoryManager.getCategoryById(categoryId, callback);
    }

    public void searchCategories(String query, BaseRepository.RepositoryCallback<List<Category>> callback) {
        categoryManager.searchCategories(query, callback);
    }

    public void validateCategoryName(String name, String currentCategoryId, BaseRepository.RepositoryCallback<Boolean> callback) {
        categoryManager.validateCategoryName(name, currentCategoryId, callback);
    }

    // === UTILITIES ===
    public List<Category> getCategories() {
        return new ArrayList<>(categories);
    }

    public boolean isDefaultCategory(Category category) {
        return categoryManager.isDefaultCategory(category);
    }

    public Category createDefaultCategory(String name, String color, String icon) {
        return categoryManager.createDefaultCategory(name, color, icon);
    }

    // === MISSING METHODS ===
    public void createCategory(Category category, CategoryOperationCallback callback) {
        addCategory(category, callback);
    }
    
    public void initializeDefaultCategories(CategoryOperationCallback callback) {
        categoryManager.initializeDefaultCategories(new BaseRepository.DatabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (callback != null) callback.onSuccess();
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
            }
        });
    }
    
    public void clearAllCategories(CategoryOperationCallback callback) {
        getAllCategories(new BaseRepository.ListCallback<Category>() {
            @Override
            public void onSuccess(List<Category> categoriesList) {
                clearCategoriesSequentially(categoriesList, 0, callback);
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
            }
        });
    }
    
    private void clearCategoriesSequentially(List<Category> categoriesList, int index, CategoryOperationCallback callback) {
        if (index >= categoriesList.size()) {
            if (callback != null) callback.onSuccess();
            return;
        }
        
        Category category = categoriesList.get(index);
        deleteCategory(category, new CategoryOperationCallback() {
            @Override
            public void onSuccess() {
                clearCategoriesSequentially(categoriesList, index + 1, callback);
            }

            @Override
            public void onError(String error) {
                // Continue with next category even if one fails
                clearCategoriesSequentially(categoriesList, index + 1, callback);
            }
        });
    }
    
    public void initializeDefaultCategories() {
        initializeDefaultCategories(null);
    }
    
    public void clearAllDataAndReset() {
        categoryManager.removeDuplicateCategories(new BaseRepository.DatabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                initializeDefaultCategories();
            }

            @Override
            public void onError(String error) {
                if (listener != null) {
                    listener.onError("Lỗi reset data: " + error);
                }
            }
        });
    }

    private void showToast(String message) {
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    public void cleanup() {
        if (realtimeListener != null) {
            categoryRepository.removeCategoriesListener(realtimeListener);
        }
    }
}
