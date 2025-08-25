package com.example.todolist.service;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
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
    private CategoryManager categoryManager;
    private Handler firebaseUpdateHandler;
    private List<Category> allCategories;
    private Runnable pendingFirebaseUpdate;
    private long lastLocalUpdateTime;
    private static final long LOCAL_UPDATE_PRIORITY_WINDOW = 1000;
    private static final long FIREBASE_UPDATE_DELAY = 500;

    public CategoryService(Context context, CategoryUpdateListener listener) {
        this.context = context;
        this.listener = listener;
        this.categoryRepository = new CategoryRepository();
        this.firebaseUpdateHandler = new Handler(Looper.getMainLooper());
        
        // Initialize sub-services
        this.categoryManager = new CategoryManager(context);
        this.allCategories = new ArrayList<>();
    }

    public void loadCategories() {
        categoryManager.removeDuplicateCategories(new BaseRepository.DatabaseCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
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
                handleFirebaseCategoriesUpdate(categoriesList);
            }

            @Override
            public void onError(String error) {
                if (listener != null) {
                    listener.onError("Loi tai categories: " + error);
                }
            }
        });
    }

    private void handleFirebaseCategoriesUpdate(List<Category> categories) {
        if (pendingFirebaseUpdate != null) {
            firebaseUpdateHandler.removeCallbacks(pendingFirebaseUpdate);
        }
        
        pendingFirebaseUpdate = () -> {
            long timeSinceLastLocalUpdate = System.currentTimeMillis() - lastLocalUpdateTime;
            if (timeSinceLastLocalUpdate < LOCAL_UPDATE_PRIORITY_WINDOW) {
                pendingFirebaseUpdate = null;
                return;
            }
            
            allCategories = categories;
            notifyListener();
            pendingFirebaseUpdate = null;
        };
        
        firebaseUpdateHandler.postDelayed(pendingFirebaseUpdate, FIREBASE_UPDATE_DELAY);
    }

    private void notifyListener() {
        if (listener != null) {
            listener.onCategoriesUpdated();
        }
    }

    public void addCategory(Category category) {
        addCategory(category, null);
    }
    
    public void addCategory(Category category, CategoryOperationCallback callback) {
        if (category.getId() == null || category.getId().isEmpty()) {
            category.setId(String.valueOf(System.currentTimeMillis()) + "_" + Math.random());
        }
        
        lastLocalUpdateTime = System.currentTimeMillis();
        
        categoryManager.addCategory(category, new BaseRepository.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String categoryId) {
                if (!category.getId().equals(categoryId)) {
                    category.setId(categoryId);
                }
                if (callback != null) callback.onSuccess();
            }

            @Override
            public void onError(String error) {
                if (callback != null) callback.onError(error);
            }
        });
    }

    public void updateCategory(Category category) {
        updateCategory(category, null);
    }
    
    public void updateCategory(Category category, CategoryOperationCallback callback) {
        lastLocalUpdateTime = System.currentTimeMillis();
        
        categoryManager.updateCategory(category, new BaseRepository.DatabaseCallback<Boolean>() {
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

    public void deleteCategory(Category category) {
        deleteCategory(category, null);
    }
    
    public void deleteCategory(Category category, CategoryOperationCallback callback) {
        lastLocalUpdateTime = System.currentTimeMillis();
        
        categoryManager.deleteCategory(category, new BaseRepository.DatabaseCallback<Boolean>() {
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

    public Category getCategoryByIdFromCache(String categoryId) {
        // Return from allCategories list instead of cache
        for (Category category : allCategories) {
            if (category.getId().equals(categoryId)) {
                return category;
            }
        }
        return null;
    }

    public void searchCategories(String query, BaseRepository.RepositoryCallback<List<Category>> callback) {
        categoryManager.searchCategories(query, callback);
    }

    public List<Category> searchCategoriesFromCache(String query) {
        List<Category> result = new ArrayList<>();
        for (Category category : allCategories) {
            if (category.getName().toLowerCase().contains(query.toLowerCase())) {
                result.add(category);
            }
        }
        return result;
    }

    public void validateCategoryName(String name, String currentCategoryId, BaseRepository.RepositoryCallback<Boolean> callback) {
        categoryManager.validateCategoryName(name, currentCategoryId, callback);
    }

    // === UTILITIES ===
    public List<Category> getCategories() {
        return new ArrayList<>(allCategories);
    }

    public List<Category> getAllCategoriesFromCache() {
        return new ArrayList<>(allCategories);
    }

    public List<Category> getCategoriesByColorFromCache(String color) {
        List<Category> result = new ArrayList<>();
        for (Category category : allCategories) {
            if (category.getColor().equals(color)) {
                result.add(category);
            }
        }
        return result;
    }

    public List<Category> getDefaultCategoriesFromCache() {
        List<Category> result = new ArrayList<>();
        for (Category category : allCategories) {
            if (category.isDefault()) {
                result.add(category);
            }
        }
        return result;
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
                    listener.onError("Loi reset data: " + error);
                }
            }
        });
    }

    public void cleanup() {
        if (realtimeListener != null) {
            categoryRepository.removeCategoriesListener(realtimeListener);
        }
    }

    private void cancelPendingFirebaseUpdates() {
        if (pendingFirebaseUpdate != null) {
            firebaseUpdateHandler.removeCallbacks(pendingFirebaseUpdate);
            pendingFirebaseUpdate = null;
        }
    }
}
